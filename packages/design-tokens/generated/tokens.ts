// GENERATED — DO NOT EDIT
export const tokens = {
  "colors": {
    "primary": "#0B6B47",
    "primaryStrong": "#06452F",
    "primaryDeep": "#043526",
    "primarySoft": "#DCEFE6",
    "primarySofter": "#F0F8F4",
    "barDeep": "#042623",
    "barContent": "#FFFFFF",
    "barContentMuted": "#9FBDB6",
    "background": "#F7F8F5",
    "surface": "#FFFFFF",
    "surfaceAlt": "#F1F4F2",
    "textPrimary": "#15231C",
    "textSecondary": "#5D6B64",
    "textMuted": "#7A8780",
    "border": "#DDE4E0",
    "borderStrong": "#C5D0CA",
    "success": "#138A5B",
    "warning": "#C98214",
    "danger": "#C23B3B",
    "info": "#2E6FB5"
  },
  "semantic": {
    "content": {
      "primary": "{colors.textPrimary}",
      "secondary": "{colors.textSecondary}",
      "muted": "{colors.textMuted}",
      "onPrimary": "{colors.surface}",
      "danger": "{colors.danger}",
      "onBar": "{colors.barContent}",
      "onBarMuted": "{colors.barContentMuted}"
    },
    "surface": {
      "canvas": "{colors.background}",
      "default": "{colors.surface}",
      "subtle": "{colors.surfaceAlt}",
      "brandSoft": "{colors.primarySoft}",
      "bar": "{colors.barDeep}"
    },
    "action": {
      "primary": "{colors.primary}",
      "primaryPressed": "{colors.primaryStrong}",
      "danger": "{colors.danger}"
    },
    "stroke": {
      "default": "{colors.border}",
      "strong": "{colors.borderStrong}"
    },
    "feedback": {
      "success": "{colors.success}",
      "warning": "{colors.warning}",
      "danger": "{colors.danger}",
      "info": "{colors.info}"
    }
  },
  "typography": {
    "fontFamily": {
      "primary": "Tajawal",
      "fallback": "Arial, sans-serif"
    },
    "roles": {
      "display": {
        "size": 32,
        "lineHeight": 40,
        "weight": 700
      },
      "headlineLarge": {
        "size": 28,
        "lineHeight": 36,
        "weight": 700
      },
      "headlineMedium": {
        "size": 24,
        "lineHeight": 32,
        "weight": 700
      },
      "titleLarge": {
        "size": 20,
        "lineHeight": 28,
        "weight": 700
      },
      "titleMedium": {
        "size": 18,
        "lineHeight": 26,
        "weight": 600
      },
      "bodyLarge": {
        "size": 16,
        "lineHeight": 26,
        "weight": 400
      },
      "bodyMedium": {
        "size": 14,
        "lineHeight": 22,
        "weight": 400
      },
      "bodySmall": {
        "size": 12,
        "lineHeight": 18,
        "weight": 400
      },
      "labelLarge": {
        "size": 14,
        "lineHeight": 20,
        "weight": 600
      },
      "labelMedium": {
        "size": 12,
        "lineHeight": 18,
        "weight": 600
      }
    }
  },
  "spacing": {
    "2xs": 2,
    "xs": 4,
    "sm": 8,
    "md": 12,
    "base": 16,
    "lg": 20,
    "xl": 24,
    "2xl": 32,
    "3xl": 40,
    "4xl": 48,
    "5xl": 64
  },
  "radius": {
    "small": 8,
    "medium": 12,
    "large": 16,
    "xl": 20,
    "pill": 999
  },
  "elevation": {
    "none": {
      "y": 0,
      "blur": 0,
      "spread": 0,
      "opacity": 0
    },
    "low": {
      "y": 1,
      "blur": 3,
      "spread": 0,
      "opacity": 0.08
    },
    "medium": {
      "y": 4,
      "blur": 12,
      "spread": 0,
      "opacity": 0.1
    }
  },
  "motion": {
    "duration": {
      "feedback": 120,
      "standard": 220,
      "emphasized": 250
    },
    "easing": {
      "standard": "cubic-bezier(0.2, 0, 0, 1)",
      "decelerate": "cubic-bezier(0, 0, 0, 1)"
    }
  }
} as const;
