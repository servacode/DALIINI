import type { Category, Province } from "../lib/api";
import { Icon } from "./ui";

/*
 * Plain GET forms to /search: they work without JavaScript and the results
 * page renders on the server. `id` keeps labels unique when two forms share a
 * page (the header and the home hero).
 */

export const SEARCH_PLACEHOLDER = "صيدلية، عيادة، اسم حي…";

/** A single search field with its button: the header and the home hero. */
export function SearchBox({ id, variant = "hero", defaultValue }: { id: string; variant?: "header" | "hero"; defaultValue?: string }) {
  return (
    <form role="search" action="/search" method="get" className={`search-box search-${variant}`}>
      <label htmlFor={id} className="sr-only">ابحث في دليني</label>
      <input
        id={id}
        name="q"
        type="search"
        defaultValue={defaultValue}
        placeholder={SEARCH_PLACEHOLDER}
        autoComplete="off"
        enterKeyHint="search"
        required
        minLength={2}
      />
      <button type="submit" className="button">
        <Icon name="search" />
        <span className={variant === "header" ? "sr-only" : undefined}>ابحث</span>
      </button>
    </form>
  );
}

/** The full form on /search: term, province and category. */
export function SearchForm({
  q,
  province,
  category,
  provinces,
  categories,
}: {
  q: string;
  province?: string;
  category?: string;
  provinces: Province[];
  categories: Category[];
}) {
  return (
    <form role="search" action="/search" method="get" className="card search-form">
      <div className="field field-wide">
        <label htmlFor="search-q">ما الذي تبحث عنه؟</label>
        <input
          id="search-q"
          name="q"
          type="search"
          defaultValue={q}
          placeholder={SEARCH_PLACEHOLDER}
          autoComplete="off"
          enterKeyHint="search"
          required
          minLength={2}
        />
      </div>
      {provinces.length > 1 ? (
        <div className="field">
          <label htmlFor="search-province">المحافظة</label>
          <select id="search-province" name="province" defaultValue={province ?? ""}>
            <option value="">كل المحافظات</option>
            {provinces.map((p) => <option key={p.id} value={p.code}>{p.nameAr}</option>)}
          </select>
        </div>
      ) : null}
      {categories.length > 0 ? (
        <div className="field">
          <label htmlFor="search-category">التصنيف</label>
          <select id="search-category" name="category" defaultValue={category ?? ""}>
            <option value="">كل التصنيفات</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.nameAr}</option>)}
          </select>
        </div>
      ) : null}
      <button type="submit" className="button">
        <Icon name="search" />
        ابحث
      </button>
    </form>
  );
}
