import { useState } from 'react';
import { USER } from '../data/courseState';

export default function CourseComments({ course, execute }) {
  const [text, setText] = useState('');
  const [notice, setNotice] = useState('');
  const [deleting, setDeleting] = useState(null);
  return <section className="panel"><div className="panel-heading"><h2>댓글 <span className="muted">({course.comments.length}개)</span></h2></div>
    <ul className="comment-list">{course.comments.length ? course.comments.map((comment) => <li key={comment.id}><span><strong>{comment.author} </strong>{comment.text}</span>{(comment.mine || comment.author === USER) && (deleting === comment.id ? <div className="actions"><span>삭제할까요?</span><button onClick={async () => { const result = await execute?.({ type: 'delete-comment', id: course.id, commentId: comment.id }); setNotice(result?.error || '댓글을 삭제했습니다.'); setDeleting(null); }}>삭제 확인</button><button onClick={() => setDeleting(null)}>취소</button></div> : <button onClick={() => setDeleting(comment.id)}>내 댓글 삭제</button>)}</li>) : <li>등록된 댓글이 없습니다.</li>}</ul>
    <form onSubmit={async (event) => { event.preventDefault(); const result = await execute?.({ type: 'comment', id: course.id, commentId: crypto.randomUUID(), text }); setNotice(result?.error || '댓글을 등록했습니다.'); if (!result?.error) setText(''); }}><div className="field"><label>댓글 작성<textarea name="comment" rows={3} maxLength={300} required value={text} onChange={(event) => setText(event.target.value)} placeholder="댓글을 입력해 보세요." /></label></div><button className="primary" type="submit">댓글 등록</button></form>
    {notice && <p className="status" role="status">{notice}</p>}
  </section>;
}
