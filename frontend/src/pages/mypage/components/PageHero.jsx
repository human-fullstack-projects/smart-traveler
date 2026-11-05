export default function PageHero({ eyebrow, title, children }) {
  return <div className="hero"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{children}</p></div>;
}
