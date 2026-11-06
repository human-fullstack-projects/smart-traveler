import { useEffect, useState } from 'react';
import { getOwnerReview } from '../../api/auth';
import { useAuth } from '../../contexts/AuthContext';
import OwnerReviewPage from '../auth/OwnerReviewPage';
import '../../css/Auth.css';

function AccessNotice({ title, message, href = '#/', label = '홈으로' }) {
  return (
    <div className="st-page auth-page owner-auth-page">
      <section className="auth-wrap st-panel" aria-labelledby="owner-access-title">
        <h1 id="owner-access-title" className="st-h2">{title}</h1>
        <p className="auth-notice" role="status">{message}</p>
        <a className="st-btn st-btn-line" href={href}>{label}</a>
      </section>
    </div>
  );
}

function OwnerApproval({ children }) {
  const [result, setResult] = useState({ loading: true });

  useEffect(() => {
    let cancelled = false;

    getOwnerReview()
      .then((review) => {
        if (!cancelled) setResult({ status: review.status });
      })
      .catch(() => {
        if (!cancelled) setResult({ failed: true });
      });

    return () => { cancelled = true; };
  }, []);

  if (result.loading) {
    return <AccessNotice title="업주 승인 확인" message="심사 상태를 확인하고 있어요." />;
  }

  if (result.status === 'APPROVED') return children;

  if (result.status === 'PENDING' || result.status === 'REJECTED') {
    return <OwnerReviewPage onContinue={() => { window.location.hash = '/'; }} />;
  }

  return (
    <AccessNotice
      title="업주센터에 접근할 수 없어요"
      message="승인 상태를 확인하지 못했어요. 잠시 후 다시 들어와 주세요."
    />
  );
}

export default function OwnerAccess({ children }) {
  const { member, authChecked, authCheckFailed } = useAuth();

  if (!authChecked) {
    return <AccessNotice title="업주센터" message="로그인 상태를 확인하고 있어요." />;
  }

  if (authCheckFailed) {
    return <AccessNotice title="로그인 확인 실패" message="서버 연결을 확인한 뒤 다시 시도해 주세요." />;
  }

  if (!member) {
    return (
      <AccessNotice
        title="로그인이 필요해요"
        message="업주 계정으로 로그인해 주세요."
        href="#/login"
        label="로그인"
      />
    );
  }

  if (member.roleCode !== 'OWNER') {
    return <AccessNotice title="업주 전용 화면" message="업주 계정으로 이용할 수 있어요." />;
  }

  return <OwnerApproval key={member.memberId}>{children}</OwnerApproval>;
}