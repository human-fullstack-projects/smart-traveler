export default function PageHeading({ title, description, breadcrumb, children }) {
  return <>
    {breadcrumb && <p className="breadcrumb"><a href="#/human-courses">코스 목록</a> / {breadcrumb}</p>}
    <div className="page-heading"><div><p className="small-kicker">HUMAN COURSES</p><h1>{title}</h1><p className="subheading">{description}</p></div>{children}</div>
  </>;
}
