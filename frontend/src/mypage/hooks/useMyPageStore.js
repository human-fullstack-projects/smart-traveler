import { useEffect, useReducer, useState } from 'react';
import { initialState, loadState, reduceState, STORAGE_KEY } from '../data/store';

export default function useMyPageStore() {
  const [state, dispatch] = useReducer(reduceState, undefined, () => {
    try { return loadState(window.localStorage); } catch { return initialState(); }
  });
  const [storageError, setStorageError] = useState('');
  useEffect(() => {
    try { window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state)); setStorageError(''); }
    catch { setStorageError('브라우저에 저장하지 못했습니다. 현재 변경은 새로고침하면 사라질 수 있습니다.'); }
  }, [state]);
  return { ...state, dispatch, storageError };
}
