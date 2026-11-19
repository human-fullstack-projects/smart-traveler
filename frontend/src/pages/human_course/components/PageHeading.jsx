export default function PageHeading({ title, description, breadcrumb, listLabel = '코스 목록', kicker = 'HUMAN COURSES', children }) {
  return <>
    {breadcrumb && <p className="breadcrumb"><a href="#/human-courses">{listLabel}</a> / {breadcrumb}</p>}
    <div className="page-heading"><div><p className="small-kicker">{kicker}</p><h1>{title}</h1><p className="subheading">{description}</p></div>{children}</div>
  </>;
}
