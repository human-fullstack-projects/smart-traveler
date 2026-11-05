import Header from '../../../common_components/Header';
import Footer from '../../../common_components/Footer';

// App이 헤더·푸터를 제공하는 경우에는 이 레이아웃을 중복으로 감싸지 않습니다.
export default function HumanCourseLayout({ children, route, onUnavailable }) {
  return <div className="app-shell">
    <Header route={route} onUnavailable={onUnavailable || (() => {})} />
    <main className="main-content"><div className="human-courses">{children}</div></main>
    <Footer />
  </div>;
}
