"use client";

import { type ReactNode, useSyncExternalStore } from "react";

/*
 * Renders its children only in an Android browser. «افتح في التطبيق» is an Android
 * intent link: on iOS or a desktop it would do nothing (or reload the page), so it is
 * not offered there. Pages are cached, so the server cannot know the device; the check
 * runs in the browser, and the server snapshot is "not Android", so nothing flashes.
 */
const subscribe = () => () => undefined;

export function AndroidOnly({ children }: { children: ReactNode }) {
  const android = useSyncExternalStore(
    subscribe,
    () => /android/i.test(navigator.userAgent),
    () => false,
  );
  return android ? <>{children}</> : null;
}
