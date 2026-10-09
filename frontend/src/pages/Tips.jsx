import useFetch from '../hooks/useFetch';
import { Loading, Notice, PageHeader } from '../components/ui';

export default function Tips() {
  const { data, loading, error } = useFetch('/tips');
  return (
    <>
      <PageHeader title="Water-saving tips" subtitle="Small habits that reduce your bill" />
      <Notice>{error}</Notice>
      {loading ? <Loading /> : (
        <div className="tips big">
          {data.map((t) => <div key={t.id} className="tip card"><h3>💡 {t.title}</h3><p>{t.content}</p></div>)}
        </div>
      )}
    </>
  );
}
