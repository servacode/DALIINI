import { ResponseError } from "@servacode/api-typescript";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { AdminApis } from "../../lib/api/client";
import {
  MAX_AD_IMAGE_BYTES,
  UPLOAD_MESSAGES,
  arabicUploadFailure,
  checkAdImage,
  readBounded,
  sniffImage,
} from "../../lib/http/upload";

/**
 * The advertisement image upload: the only BFF route that takes a file.
 *
 * What it must hold: the same-origin rule for a cookie-authenticated POST (with multipart
 * allowed here and nowhere else), a hard ceiling on what it reads, refusal of anything that
 * is not a JPEG, PNG or WebP by its bytes, and an Arabic answer for every refusal — including
 * Django's own, which arrive in English.
 */

const uploads: Blob[] = [];
let upstream: () => Promise<unknown>;

const callWithSession = vi.fn(async (run: (apis: AdminApis) => Promise<unknown>) => {
  const apis = {
    ads: {
      adminAdImageUpload: async ({ file }: { file: Blob }) => {
        uploads.push(file);
        return upstream();
      },
    },
  } as unknown as AdminApis;
  try {
    return { ok: true as const, data: await run(apis) };
  } catch (error) {
    if (error instanceof ResponseError) {
      return {
        ok: false as const,
        status: error.response.status,
        body: (await error.response.json()) as Record<string, unknown>,
      };
    }
    throw error;
  }
});

vi.mock("../../lib/auth/session", () => ({ callWithSession }));

const { POST } = await import("../../app/api/admin/ads/images/route");

const PNG_HEAD = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a];
const png = (size = 64) =>
  new File([new Uint8Array([...PNG_HEAD, ...new Array(size).fill(0)])], "slide.png", {
    type: "image/png",
  });

function upload(
  body: BodyInit | null,
  headers: Record<string, string> = {},
): Promise<Response> {
  return POST(
    new Request("https://admin.example.com/api/admin/ads/images", {
      method: "POST",
      headers: { host: "admin.example.com", origin: "https://admin.example.com", ...headers },
      body,
      // Required by Node's fetch whenever the body is a stream rather than a buffer, and
      // harmless for the cases that pass one.
      ...(body instanceof ReadableStream ? { duplex: "half" } : {}),
    } as RequestInit),
  );
}

function form(entry?: File | string): FormData {
  const data = new FormData();
  if (entry !== undefined) data.append("file", entry);
  return data;
}

beforeEach(() => {
  uploads.length = 0;
  callWithSession.mockClear();
  upstream = async () => ({
    imageKey: "ads/abc.jpg",
    url: "https://media.example.com/ads/abc.jpg",
    width: 1600,
    height: 900,
  });
});

describe("the checks before anything is forwarded", () => {
  it("recognises the three formats by their bytes, not their name", () => {
    expect(sniffImage(new Uint8Array([0xff, 0xd8, 0xff, 0xe0]))).toBe("jpeg");
    expect(sniffImage(new Uint8Array(PNG_HEAD))).toBe("png");
    expect(sniffImage(new TextEncoder().encode("RIFF\0\0\0\0WEBPVP8 "))).toBe("webp");
    expect(sniffImage(new TextEncoder().encode("%PDF-1.7"))).toBeNull();
    expect(sniffImage(new Uint8Array([0xff]))).toBeNull();
  });

  it("refuses a missing, empty, oversized or non-image part", async () => {
    expect((await checkAdImage(null))?.body.message).toBe(UPLOAD_MESSAGES.missing);
    expect((await checkAdImage("text"))?.body.message).toBe(UPLOAD_MESSAGES.missing);
    expect((await checkAdImage(new File([], "a.png")))?.body.message).toBe(UPLOAD_MESSAGES.empty);
    const huge = new File([new Uint8Array(MAX_AD_IMAGE_BYTES + 1)], "big.png");
    expect(await checkAdImage(huge)).toMatchObject({ status: 413 });
    const pdf = new File([new TextEncoder().encode("%PDF-1.7 ...")], "renamed.png", {
      type: "image/png",
    });
    expect((await checkAdImage(pdf))?.body.details).toEqual({ file: [UPLOAD_MESSAGES.format] });
    expect(await checkAdImage(png())).toBeNull();
  });

  it("reads no more than the ceiling, with or without a declared length", async () => {
    const declared = new Request("https://a.example/", {
      method: "POST",
      headers: { "content-length": "999999999" },
      body: "x",
    });
    expect(await readBounded(declared, 10)).toBeNull();

    const streamed = new Request("https://a.example/", {
      method: "POST",
      body: new ReadableStream({
        start(controller) {
          controller.enqueue(new Uint8Array(8));
          controller.enqueue(new Uint8Array(8));
          controller.close();
        },
      }),
      duplex: "half",
    } as RequestInit);
    expect(await readBounded(streamed, 10)).toBeNull();

    const small = new Request("https://a.example/", { method: "POST", body: "abc" });
    expect(Array.from((await readBounded(small, 10)) ?? [])).toEqual([97, 98, 99]);
  });

  it("restates Django's English refusals of the file in Arabic", () => {
    const refusal = (reason: string) =>
      arabicUploadFailure({
        code: "VALIDATION_ERROR",
        message: "Request validation failed.",
        details: { file: [reason] },
        requestId: "req-1",
      });

    expect(refusal("Each side must be between 100 and 4096 pixels.").message).toBe(
      UPLOAD_MESSAGES.dimensions,
    );
    expect(refusal("The image is larger than 2 MB.").details).toEqual({
      file: [UPLOAD_MESSAGES.tooLarge],
    });
    expect(refusal("The file is not a valid image.").message).toBe(UPLOAD_MESSAGES.invalid);
    expect(refusal("The file is not a valid image.").requestId).toBe("req-1");
    const denied = {
      code: "PERMISSION_DENIED",
      message: "",
      details: {},
      requestId: "",
    };
    expect(arabicUploadFailure(denied)).toBe(denied);
  });
});

describe("POST /api/admin/ads/images", () => {
  it("refuses another origin before reading the body", async () => {
    vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const response = await upload(form(png()), { origin: "https://evil.example" });

    expect(response.status).toBe(403);
    expect(((await response.json()) as { code: string }).code).toBe("ORIGIN_REJECTED");
    expect(callWithSession).not.toHaveBeenCalled();
  });

  it("refuses a body that is not multipart", async () => {
    vi.spyOn(console, "warn").mockImplementation(() => undefined);
    const response = await upload(JSON.stringify({ file: "x" }), {
      "content-type": "application/json",
    });

    expect(response.status).toBe(403);
    expect(callWithSession).not.toHaveBeenCalled();
  });

  it("answers a missing file or a non-image in Arabic, without calling Django", async () => {
    const missing = await upload(form());
    const pdf = await upload(
      form(new File([new TextEncoder().encode("%PDF-1.7")], "a.pdf", { type: "application/pdf" })),
    );

    expect(missing.status).toBe(400);
    expect(((await missing.json()) as { message: string }).message).toBe(UPLOAD_MESSAGES.missing);
    expect(pdf.status).toBe(400);
    expect(((await pdf.json()) as { details: unknown }).details).toEqual({
      file: [UPLOAD_MESSAGES.format],
    });
    expect(callWithSession).not.toHaveBeenCalled();
  });

  /*
   * The body is a stream this test owns rather than one built from a `File`.
   *
   * The route answers an oversized body by cancelling the stream half-read, which is the right
   * thing to do — it is what stops the server reading a gigabyte it has already refused. Node's
   * own fetch implementation reacts to that by continuing to write into the stream the route has
   * just closed, which surfaces as an unhandled rejection attributed to whichever test runs
   * next. Owning the producer means the cancellation stops it, and the test measures the route
   * instead of the runtime.
   */
  it("refuses an oversized body with 413, and stops reading it", async () => {
    const chunk = new Uint8Array(256 * 1024);
    let written = 0;
    let cancelled = false;
    const body = new ReadableStream({
      pull(controller) {
        if (written > MAX_AD_IMAGE_BYTES + 1024 * 1024) {
          controller.close();
          return;
        }
        written += chunk.byteLength;
        controller.enqueue(chunk);
      },
      cancel() {
        cancelled = true;
      },
    });

    const response = await upload(body, { "content-type": "multipart/form-data" });

    expect(response.status).toBe(413);
    expect(callWithSession).not.toHaveBeenCalled();
    // It gave up rather than reading everything that was offered.
    expect(cancelled).toBe(true);
    expect(written).toBeLessThanOrEqual(MAX_AD_IMAGE_BYTES + chunk.byteLength);
  });


  it("forwards a valid image and answers with its key", async () => {
    const response = await upload(form(png()));

    expect(response.status).toBe(201);
    expect(await response.json()).toEqual({
      imageKey: "ads/abc.jpg",
      url: "https://media.example.com/ads/abc.jpg",
      width: 1600,
      height: 900,
    });
    expect(uploads).toHaveLength(1);
    expect(uploads[0]!.size).toBe(PNG_HEAD.length + 64);
  });

  it("passes Django's refusal on in Arabic", async () => {
    upstream = async () => {
      throw new ResponseError(
        new Response(
          JSON.stringify({
            code: "VALIDATION_ERROR",
            message: "Request validation failed.",
            details: { file: ["Each side must be between 100 and 4096 pixels."] },
            requestId: "req-2",
          }),
          { status: 400, headers: { "Content-Type": "application/json" } },
        ),
      );
    };

    const response = await upload(form(png()));

    expect(response.status).toBe(400);
    expect(await response.json()).toMatchObject({
      message: UPLOAD_MESSAGES.dimensions,
      details: { file: [UPLOAD_MESSAGES.dimensions] },
      requestId: "req-2",
    });
  });
});
