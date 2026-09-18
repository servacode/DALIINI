import { callWithSession } from "../../../../lib/auth/session";
import {
  READS,
  type ReadFn,
  WRITES,
  type WriteFn,
  isReadOperation,
  isWriteOperation,
} from "../../../../lib/api/operations";
import { checkSameOrigin } from "../../../../lib/http/csrf";
import { fail, ok, readJson, refuseOrigin } from "../../../../lib/http/responses";

/**
 * The BFF's only data route.
 *
 * The browser names an operation; this looks it up in the registry and runs the generated
 * client method behind it. An unknown name is a 404 before anything reaches the network, so
 * the reachable surface is exactly what `lib/api/operations.ts` declares and nothing else.
 *
 * Reads are `GET` and writes are `POST`, on separate registries. A mutation therefore has
 * no `GET` form at all, which means a link, a prefetch or an `<img src>` cannot trigger one
 * — and the origin check, which deliberately leaves safe methods alone, is never asked to
 * carry that weight.
 */

type Context = { params: Promise<{ operation: string }> };

export async function GET(request: Request, context: Context): Promise<Response> {
  const { operation } = await context.params;
  if (!isReadOperation(operation)) {
    return fail(404, {
      code: "NOT_FOUND",
      message: "العنصر المطلوب غير موجود.",
      details: {},
      requestId: "",
    });
  }

  const params = Object.fromEntries(new URL(request.url).searchParams.entries());
  // The registry is a union of differently-shaped operations; each one is called through
  // its common signature so the route stays generic over all of them.
  const run = READS[operation] as ReadFn;
  const outcome = await callWithSession((apis) => run(apis, params));
  return outcome.ok ? ok(outcome.data) : fail(outcome.status, outcome.body);
}

export async function POST(request: Request, context: Context): Promise<Response> {
  const { operation } = await context.params;
  const rejection = checkSameOrigin(request);
  if (rejection) return refuseOrigin(rejection, `/api/admin/${operation}`);

  if (!isWriteOperation(operation)) {
    return fail(404, {
      code: "NOT_FOUND",
      message: "العنصر المطلوب غير موجود.",
      details: {},
      requestId: "",
    });
  }

  const body = (await readJson(request)) ?? {};
  const run = WRITES[operation] as WriteFn;
  const outcome = await callWithSession((apis) => run(apis, body));
  // A mutation that answers 204 has no body; `{ok: true}` keeps the browser side uniform.
  return outcome.ok
    ? ok(outcome.data ?? { ok: true })
    : fail(outcome.status, outcome.body);
}
