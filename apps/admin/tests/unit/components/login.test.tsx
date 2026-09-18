import { cleanup, render, screen } from "@testing-library/react";
import { renderToString } from "react-dom/server";
import { afterEach, describe, expect, it, vi } from "vitest";

import { LoginForm } from "../../../components/auth/login-form";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: vi.fn(), refresh: vi.fn() }),
}));

afterEach(cleanup);

/**
 * What the browser has before React hydrates it.
 *
 * Under load, the first Playwright run clicked submit before the handler was attached, the
 * browser submitted the form itself by GET, and the password landed in the URL. These
 * assertions are made on the server-rendered HTML, because that is the page an operator
 * actually holds during that window.
 */
describe("login form before hydration", () => {
  const html = renderToString(<LoginForm />);

  it("posts rather than gets, so credentials never enter a URL", () => {
    expect(html).toMatch(/<form[^>]*method="post"/);
  });

  it("renders the submit button disabled until React owns the page", () => {
    expect(html).toMatch(/<button[^>]*type="submit"[^>]*disabled=""/);
  });
});

describe("login form after hydration", () => {
  it("enables the submit button once mounted on the client", () => {
    render(<LoginForm />);

    expect(screen.getByTestId("login-submit").hasAttribute("disabled")).toBe(false);
  });
});
