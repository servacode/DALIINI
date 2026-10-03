package com.servacode.directory.core.designsystem

/**
 * The icons this module draws itself, for concepts the shared set (packages/design-tokens/icons)
 * does not have yet: the travel modes, a filled star, a camera. Their paths are the vector
 * drawables Android drew before the design system was shared (DECISION-094), unchanged, in the
 * same colours of the theme.
 */
internal object ModuleGlyphs {
    val camera = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M3.5,8.5 L7,8.5 L8.6,6 L15.4,6 L17,8.5 L20.5,8.5 L20.5,19 L3.5,19 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M12,10.5 A3.4,3.4 0 1,0 12,17.3 A3.4,3.4 0 1,0 12,10.5 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
        ),
        mirrored = false,
    )
    val car = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M4,16.3 L4,12.2 L6.6,7.7 L17.4,7.7 L20,12.2 L20,16.3 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M4,12.2 L20,12.2", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke(
                "M7.6,18 A1.6,1.6 0 1,0 7.6,14.8 A1.6,1.6 0 1,0 7.6,18 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M16.4,18 A1.6,1.6 0 1,0 16.4,14.8 A1.6,1.6 0 1,0 16.4,18 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
        ),
        mirrored = false,
    )
    val closeBox = DirectoryGlyph(
        listOf(
            GlyphPath.fill(
                "M5,3 L19,3 A2,2 0 0,1 21,5 L21,19 A2,2 0 0,1 19,21 L5,21 A2,2 0 0,1 3,19 L3,5 A2,2 0 0,1 5,3 Z",
                GlyphRole.DANGER,
            ),
            GlyphPath.stroke(
                "M8.5,8.5 L15.5,15.5 M15.5,8.5 L8.5,15.5",
                GlyphRole.ON_PRIMARY,
                width = 2.2f,
                roundCap = true,
                roundJoin = false,
            ),
        ),
        mirrored = false,
    )
    val document = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M6,3 L14,3 L18,7 L18,21 L6,21 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M14,3 L14,7 L18,7", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M9,12 L15,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M9,16 L15,16", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val eyeOff = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M2.5,12 C5,7.5 8.4,5.5 12,5.5 C15.6,5.5 19,7.5 21.5,12 C19,16.5 15.6,18.5 12,18.5 C8.4,18.5 " +
                    "5,16.5 2.5,12 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M12,14.7 A2.7,2.7 0 1,0 12,9.3 A2.7,2.7 0 1,0 12,14.7 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M4,20 L20,4", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val hospital = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M5,20 L5,7.5 L19,7.5 L19,20",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M3,20 L21,20", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M12,10.5 L12,16", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke(
                "M9.2,13.2 L14.8,13.2",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M8,7.5 L8,4 L16,4 L16,7.5",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
        ),
        mirrored = false,
    )
    val minus = DirectoryGlyph(
        listOf(
            GlyphPath.stroke("M5,12 L19,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val motorcycle = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M5.6,19.3 A2.9,2.9 0 1,0 5.6,13.5 A2.9,2.9 0 1,0 5.6,19.3 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M18.4,19.3 A2.9,2.9 0 1,0 18.4,13.5 A2.9,2.9 0 1,0 18.4,19.3 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M5.6,16.4 L9.4,16.4 L12.2,11.6 L16.2,11.6 L18.4,16.4",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M15.4,11.6 L17,8.4 L19.8,8.4",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M8.4,12.6 L12,12.6", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val myLocation = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M12,7.5 A4.5,4.5 0 1,0 12,16.5 A4.5,4.5 0 1,0 12,7.5 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = false,
                roundJoin = false,
            ),
            GlyphPath.stroke(
                "M12,2 L12,5 M12,19 L12,22 M2,12 L5,12 M19,12 L22,12",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = false,
            ),
            GlyphPath.fill("M12,11 A1,1 0 1,0 12,13 A1,1 0 1,0 12,11 Z", GlyphRole.CONTENT),
        ),
        mirrored = false,
    )
    val nursing = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M12,9.6 C10.9,8.2 9.1,7.5 7.8,8.5 C6.5,9.5 6.6,11.3 7.9,12.4 L12,15.9 L16.1,12.4 C17.4,11.3 " +
                    "17.5,9.5 16.2,8.5 C14.9,7.5 13.1,8.2 12,9.6 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M4,17.4 C6.2,20.3 9.1,21.6 12,21.6 C14.9,21.6 17.8,20.3 20,17.4",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
        ),
        mirrored = false,
    )
    val starFilled = DirectoryGlyph(
        listOf(
            GlyphPath.fill(
                "M12,2.6 L14.9,8.5 L21.4,9.4 L16.7,14 L17.8,20.5 L12,17.4 L6.2,20.5 L7.3,14 L2.6,9.4 L9.1,8.5 Z",
                GlyphRole.CONTENT,
            ),
        ),
        mirrored = false,
    )
    val supplies = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M7.5,9.5 L16,9.5 L16,14.5 L7.5,14.5 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M16,12 L21,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M7.5,12 L3.5,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M7.5,8 L7.5,16", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M10.5,9.5 L10.5,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M13,9.5 L13,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val timeSlot = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M4,7 L20,7 L20,20 L4,20 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M4,11 L20,11", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M8,4 L8,8", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
            GlyphPath.stroke("M16,4 L16,8", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
    val walk = DirectoryGlyph(
        listOf(
            GlyphPath.stroke(
                "M13.3,6.6 A1.8,1.8 0 1,0 13.3,3.0 A1.8,1.8 0 1,0 13.3,6.6 Z",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M12.6,9.2 L10.9,13.4 L13.6,15.8 L14.4,20.4",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke(
                "M10.9,13.4 L8.4,17 L7.8,20.4",
                GlyphRole.CONTENT,
                width = 2.0f,
                roundCap = true,
                roundJoin = true,
            ),
            GlyphPath.stroke("M12.9,10.4 L16.2,12", GlyphRole.CONTENT, width = 2.0f, roundCap = true, roundJoin = true),
        ),
        mirrored = false,
    )
}
