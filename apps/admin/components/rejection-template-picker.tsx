"use client";

import { useId } from "react";

import { useResource } from "../lib/client/use-resource";

export type RejectionTemplate = Readonly<{
  id: string;
  titleAr: string;
  bodyAr: string;
  active: boolean;
  sortOrder: number;
}>;

/**
 * Pick a ready-made rejection reason.
 *
 * Mount it in a reject dialog above the reason box: choosing a template calls
 * `onPick(bodyAr, template)`, and the screen decides what to do with the text (normally put
 * it in the box, where the reviewer can still edit it). Only active templates are offered,
 * in the order set on the templates screen.
 *
 * It is a convenience, so it never stands in the way: while loading, with no templates, or
 * without the permission to read them, it renders nothing and the reviewer types as before.
 */
export function RejectionTemplatePicker({
  onPick,
}: {
  onPick: (text: string, template: RejectionTemplate) => void;
}) {
  const templates = useResource<{ items: RejectionTemplate[] }>("rejectionTemplates", {
    active: "true",
  });
  const hintId = useId();
  const items = templates.data?.items ?? [];

  if (items.length === 0) return null;

  return (
    <label className="field" data-testid="rejection-template-picker">
      <span>قالب جاهز</span>
      <select
        // Always back on the prompt, so the same template can be chosen again after an edit.
        value=""
        aria-describedby={hintId}
        data-testid="rejection-template"
        onChange={(event) => {
          const template = items.find((item) => item.id === event.target.value);
          if (template) onPick(template.bodyAr, template);
        }}
      >
        <option value="">اختر قالباً لملء السبب</option>
        {items.map((template) => (
          <option key={template.id} value={template.id}>
            {template.titleAr}
          </option>
        ))}
      </select>
      <span className="field-hint" id={hintId}>
        يملأ القالب خانة السبب، ويمكنك تعديل النص بعدها.
      </span>
    </label>
  );
}
