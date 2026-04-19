type SectionTitleProps = {
  title: string;
  meta?: string;
};

export function SectionTitle({ title, meta }: SectionTitleProps) {
  return (
    <div className="section-title">
      <h2>{title}</h2>
      {meta ? <span className="section-title__meta">{meta}</span> : null}
    </div>
  );
}

type MetricCardProps = {
  label: string;
  value: string;
  hint: string;
};

export function MetricCard({ label, value, hint }: MetricCardProps) {
  return (
    <article className="metric-card">
      <p className="metric-card__label">{label}</p>
      <p className="metric-card__value">{value}</p>
      <p className="metric-card__hint">{hint}</p>
    </article>
  );
}

type NoticeCardProps = {
  title: string;
  body: string;
};

export function NoticeCard({ title, body }: NoticeCardProps) {
  return (
    <article className="notice-card">
      <h3>{title}</h3>
      <p>{body}</p>
    </article>
  );
}
