# Font policy

- **Display face: Alexandria** (`typography.fontFamily.display`) — page and section titles, the
  hero, large numbers. Weights 600, 700, 800.
- **Text face: IBM Plex Sans Arabic** (`typography.fontFamily.primary`) — body, labels, forms,
  tables, lists. Weights 400, 500, 600, 700.
- Each typography role in `packages/design-tokens/tokens/typography.json` names its face
  (`family: display | primary`); no surface picks a font of its own.
- Both are SIL Open Font License 1.1 and are bundled from `packages/design-tokens/fonts/`: woff2
  Arabic and Latin subsets for the web (`fonts.css`), static TTFs for Android. The licence texts
  ship beside them.
- The fallback stack (`typography.fontFamily.fallback`) is for the moment before a web font
  arrives; production clients always bundle the faces.
- Tajawal, the first brand face, was retired with identity v2 (DECISION-060).
