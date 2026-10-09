/**
 * The console's one locale for numbers and dates (the owner's decision, 2026-10-09).
 *
 * Arabic words — month and day names, «منذ», «أمس» — with **Latin digits**: «8 تشرين الأول»,
 * «12 منشأة», «+963…». Operators read phone numbers, counts and dates against each other and
 * against what they type, and those are written in Latin digits everywhere else they come
 * from. `-u-nu-latn` is the Unicode extension that keeps the language and swaps the numbering
 * system.
 *
 * Every formatter in the console takes its locale from here. It used to be the literal
 * "ar-SY" written out thirty-three times across twenty files, so changing how a number looks
 * meant finding all of them — and a missed one is a page whose digits disagree with the next.
 */
export const LOCALE = "ar-SY-u-nu-latn";

/**
 * A formatted date without the right-to-left marks the Arabic date patterns put between its
 * parts.
 *
 * With Arabic-Indic digits those marks keep «8/10/26» in order. With Latin digits they do the
 * opposite: each mark breaks the run of numbers, so a right-to-left page drew «8/10/26» as
 * «26/10/8». Without them, day, month and year form one left-to-right run, which is how a date
 * in Latin digits is read everywhere else.
 */
export function withoutDirectionMarks(text: string): string {
  return text.replace(/\u200F/g, "");
}
