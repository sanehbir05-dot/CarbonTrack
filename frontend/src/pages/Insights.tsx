import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from 'react'
import axios from 'axios'
import toast from 'react-hot-toast'
import { Send } from 'lucide-react'
import { Area, CartesianGrid, ComposedChart, Line, ReferenceDot, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { aiApi } from '../api/ai'
import { dashboardApi } from '../api/dashboard'
import { errorMessage } from '../api/client'
import type { Anomaly, AssistantResponse, ForecastResponse } from '../api/types'
import CategoryIcon from '../components/CategoryIcon'
import EmptyState from '../components/EmptyState'
import PageHead from '../components/PageHead'
import Spinner from '../components/Spinner'
import Tag from '../components/Tag'
import YearSelect, { defaultYear } from '../components/YearSelect'
import { categoryLabel, formatNumber, formatPeriod, monthName, toTonnes, unitLabel } from '../utils/format'
import { usePageTitle } from '../utils/usePageTitle'

const MONTH_CHOICES = [3, 6, 12]
const SUGGESTIONS = ['Where do most of our emissions come from?', 'Are there any unusual spikes?', 'Are we on track for our reduction target?']
const AXIS = { fontSize: 11, fill: '#8a8a88' }
const TOOLTIP_STYLE = { borderRadius: 14, border: '1px solid #ececea', boxShadow: '0 8px 24px rgba(0,0,0,.08)', fontSize: 13 }

interface Point {
  label: string
  actual?: number
  forecast?: number
  band?: [number, number]
}

/** The AI endpoints may not be deployed on every server yet. */
const isNotFound = (err: unknown) => axios.isAxiosError(err) && err.response?.status === 404
const UNAVAILABLE_TITLE = 'AI insights are not available yet'
const UNAVAILABLE_MESSAGE = 'This server does not provide the AI endpoints yet.'

const pointLabel = (year: number, month: number) => `${monthName(month)} '${String(year).slice(2)}`

/** Last 24 months of history, then the forecast joined onto the final actual point. */
function buildSeries(data: ForecastResponse): Point[] {
  const points: Point[] = data.history.slice(-24).map(h => ({ label: pointLabel(h.year, h.month), actual: toTonnes(h.totalKg) }))
  if (data.forecast.length && points.length) {
    const last = points[points.length - 1]
    last.forecast = last.actual
    last.band = [last.actual!, last.actual!]
    data.forecast.forEach(f =>
      points.push({ label: pointLabel(f.year, f.month), forecast: toTonnes(f.totalKg), band: [toTonnes(f.lowerKg), toTonnes(f.upperKg)] }),
    )
  }
  return points
}

const prettyMethod = (method: string) => {
  const text = method.toLowerCase().replace(/_/g, ' ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}

function inline(text: string): ReactNode[] {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, i) => (part.length > 4 && part.startsWith('**') && part.endsWith('**') ? <strong key={i}>{part.slice(2, -2)}</strong> : part))
}

/** Plain text with **bold** and "- " bullet lists. Nothing is injected as HTML. */
function SimpleMarkdown({ text }: { text: string }) {
  const blocks: ReactNode[] = []
  let list: string[] = []
  const flush = () => {
    if (!list.length) return
    blocks.push(
      <ul key={`u${blocks.length}`}>
        {list.map((item, i) => (
          <li key={i}>{inline(item)}</li>
        ))}
      </ul>,
    )
    list = []
  }
  text.split('\n').forEach(line => {
    const bullet = line.match(/^\s*[-*]\s+(.*)$/)
    if (bullet) {
      list.push(bullet[1])
      return
    }
    flush()
    if (line.trim()) {
      blocks.push(
        <p key={`p${blocks.length}`} style={{ marginTop: blocks.length ? 6 : 0 }}>
          {inline(line)}
        </p>,
      )
    }
  })
  flush()
  return <>{blocks}</>
}

interface Msg {
  role: 'user' | 'bot'
  text: string
  source?: AssistantResponse['source']
}

function Assistant() {
  const [msgs, setMsgs] = useState<Msg[]>([
    { role: 'bot', text: "Hi! Ask me about your emissions, anomalies or targets. I answer from your company's own data." },
  ])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const chatRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = chatRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [msgs, sending])

  async function ask(text: string) {
    const question = text.trim()
    if (!question || sending) return
    setMsgs(m => [...m, { role: 'user', text: question }])
    setInput('')
    setSending(true)
    try {
      const res = await aiApi.ask(question)
      setMsgs(m => [...m, { role: 'bot', text: res.answer, source: res.source }])
    } catch (err) {
      if (isNotFound(err)) {
        setMsgs(m => [...m, { role: 'bot', text: 'The assistant is not available on this server yet.' }])
      } else {
        toast.error(errorMessage(err, 'The assistant could not answer'))
        setMsgs(m => [...m, { role: 'bot', text: 'Sorry, I could not answer that. Please try again.' }])
      }
    } finally {
      setSending(false)
    }
  }

  const submit = (e: FormEvent) => {
    e.preventDefault()
    void ask(input)
  }

  return (
    <div className="panel">
      <div className="panel-head">
        <h3>Ask CarbonTrack</h3>
      </div>
      <div className="chat" ref={chatRef} aria-live="polite">
        {msgs.map((m, i) => (
          <div key={i} className={`msg ${m.role}`}>
            {m.role === 'bot' ? <SimpleMarkdown text={m.text} /> : m.text}
            {m.source && (
              <div className="src">
                <Tag kind={m.source === 'LLM' ? 'pink' : 'info'}>{m.source === 'LLM' ? 'Answered by LLM' : 'Rule-based answer'}</Tag>
              </div>
            )}
          </div>
        ))}
        {sending && (
          <div className="msg bot">
            <Spinner label="Thinking" />
          </div>
        )}
      </div>
      <div className="chips">
        {SUGGESTIONS.map(s => (
          <button key={s} type="button" className="chip" disabled={sending} onClick={() => void ask(s)}>
            {s}
          </button>
        ))}
      </div>
      <form className="chat-input" onSubmit={submit}>
        <input aria-label="Your question" value={input} maxLength={500} disabled={sending} onChange={e => setInput(e.target.value)} placeholder="Ask about your emissions…" />
        <button className="pill-btn dark" type="submit" aria-label="Send" disabled={sending || !input.trim()}>
          <Send size={16} strokeWidth={1.8} />
        </button>
      </form>
    </div>
  )
}

export default function Insights() {
  usePageTitle('AI insights')
  const [months, setMonths] = useState(6)
  const [forecast, setForecast] = useState<ForecastResponse | null>(null)
  const [forecastLoading, setForecastLoading] = useState(true)
  const [forecastFailed, setForecastFailed] = useState(false)
  const [aiMissing, setAiMissing] = useState(false)
  const [years, setYears] = useState<number[]>([])
  const [year, setYear] = useState<number | null>(null)
  const [anomalies, setAnomalies] = useState<Anomaly[] | null>(null)
  const [anomaliesLoading, setAnomaliesLoading] = useState(false)

  useEffect(() => {
    dashboardApi
      .years()
      .then(list => {
        setYears(list)
        setYear(defaultYear(list))
        if (!list.length) setAnomalies([])
      })
      .catch(err => {
        toast.error(errorMessage(err, 'Could not load the available years'))
        setAnomalies([])
      })
  }, [])

  useEffect(() => {
    let cancelled = false
    setForecastLoading(true)
    setForecastFailed(false)
    aiApi
      .forecast(months)
      .then(f => {
        if (!cancelled) setForecast(f)
      })
      .catch(err => {
        if (!cancelled) {
          setForecast(null)
          setForecastFailed(true)
          if (isNotFound(err)) setAiMissing(true)
          else toast.error(errorMessage(err, 'Could not load the forecast'))
        }
      })
      .finally(() => {
        if (!cancelled) setForecastLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [months])

  useEffect(() => {
    if (year === null) return
    let cancelled = false
    setAnomaliesLoading(true)
    aiApi
      .anomalies(year)
      .then(a => {
        if (!cancelled) setAnomalies(a)
      })
      .catch(err => {
        if (!cancelled) {
          setAnomalies([])
          if (isNotFound(err)) setAiMissing(true)
          else toast.error(errorMessage(err, 'Could not load anomalies'))
        }
      })
      .finally(() => {
        if (!cancelled) setAnomaliesLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [year])

  const series = useMemo(() => (forecast ? buildSeries(forecast) : []), [forecast])
  const marked = useMemo(() => {
    const byLabel = new Map(series.map(p => [p.label, p]))
    const labels = new Set((anomalies ?? []).map(a => pointLabel(a.year, a.month)))
    return [...labels].flatMap(label => {
      const p = byLabel.get(label)
      return p?.actual !== undefined ? [{ label, y: p.actual }] : []
    })
  }, [series, anomalies])

  const hasForecast = (forecast?.forecast.length ?? 0) > 0

  return (
    <>
      <PageHead title="AI insights" sub="Forecast, anomaly detection and an assistant, based on your company's own data" />

      <div className="grid-x g-main">
        <div className="panel">
          <div className="panel-head">
            <h3>Emissions forecast (tCO₂e per month)</h3>
            <div className="seg">
              {MONTH_CHOICES.map(n => (
                <button key={n} type="button" className={n === months ? 'on' : ''} aria-pressed={n === months} onClick={() => setMonths(n)}>
                  {n} months
                </button>
              ))}
            </div>
          </div>
          <div className="legend" style={{ marginBottom: 8 }}>
            <span>
              <i style={{ background: '#138a1d' }} />
              Actual
            </span>
            <span>
              <i style={{ background: '#e973a8' }} />
              Forecast and range
            </span>
            <span>
              <i style={{ background: '#c0392b', borderRadius: 9 }} />
              Anomaly
            </span>
            {forecast && <Tag kind="info">{prettyMethod(forecast.method)}</Tag>}
          </div>
          {forecastLoading && !forecast ? (
            <div className="empty">
              <Spinner label="Loading forecast" />
            </div>
          ) : forecastFailed || !forecast || series.length === 0 ? (
            aiMissing ? (
              <EmptyState title={UNAVAILABLE_TITLE} message={UNAVAILABLE_MESSAGE} />
            ) : (
              <EmptyState title="No forecast available" message="The forecast needs at least 12 months of activity data." />
            )
          ) : (
            <div style={{ opacity: forecastLoading ? 0.6 : 1, transition: 'opacity .15s' }}>
              <div style={{ height: 290 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <ComposedChart data={series} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                    <CartesianGrid vertical={false} stroke="#f0f0ee" />
                    <XAxis dataKey="label" tickLine={false} axisLine={false} tick={AXIS} minTickGap={22} />
                    <YAxis tickLine={false} axisLine={false} tick={AXIS} width={48} tickFormatter={v => formatNumber(v)} domain={['auto', 'auto']} />
                    <Tooltip
                      contentStyle={TOOLTIP_STYLE}
                      formatter={(value: number | string | Array<number | string>, name: string | number): [string, string] =>
                        Array.isArray(value)
                          ? [`${formatNumber(Number(value[0]), 1)} to ${formatNumber(Number(value[1]), 1)} tCO₂e`, String(name)]
                          : [`${formatNumber(Number(value), 1)} tCO₂e`, String(name)]
                      }
                    />
                    <Area type="monotone" dataKey="band" name="Range" stroke="none" fill="#e973a8" fillOpacity={0.18} isAnimationActive={false} />
                    <Line type="monotone" dataKey="actual" name="Actual" stroke="#138a1d" strokeWidth={2.5} dot={false} isAnimationActive={false} />
                    <Line type="monotone" dataKey="forecast" name="Forecast" stroke="#e973a8" strokeWidth={2.5} strokeDasharray="6 5" dot={false} isAnimationActive={false} />
                    {marked.map(m => (
                      <ReferenceDot key={m.label} x={m.label} y={m.y} r={6} fill="#fff" stroke="#c0392b" strokeWidth={2} />
                    ))}
                  </ComposedChart>
                </ResponsiveContainer>
              </div>
              {!hasForecast && <p className="small-note">Not enough history to forecast yet. At least 12 months of data are needed.</p>}
            </div>
          )}
        </div>

        <div className="panel">
          <div className="panel-head">
            <h3>How this works</h3>
          </div>
          <div className="alert-row" style={{ alignItems: 'flex-start' }}>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Forecast</b>
              <span className="small-note">A linear trend on monthly totals, adjusted by a seasonal factor for each calendar month. The shaded range is the forecast ± 1.96 residual standard deviations.</span>
            </div>
          </div>
          <div className="alert-row" style={{ alignItems: 'flex-start' }}>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Anomalies</b>
              <span className="small-note">A record is flagged when its quantity is 2.5 or more standard deviations from the usual for its facility and category, and marked high from 3.5.</span>
            </div>
          </div>
          <div className="alert-row" style={{ alignItems: 'flex-start' }}>
            <div>
              <b style={{ fontWeight: 500, display: 'block' }}>Assistant</b>
              <span className="small-note">Answers from a summary of your data. Each reply says whether a language model or the built-in rules produced it.</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid-x g-2">
        <div className="panel">
          <div className="panel-head">
            <h3>Detected anomalies</h3>
            {years.length > 0 && <YearSelect years={years} value={year} onChange={setYear} />}
          </div>
          {anomalies === null ? (
            <div className="empty">
              <Spinner label="Looking for anomalies" />
            </div>
          ) : anomalies.length === 0 && aiMissing ? (
            <EmptyState title={UNAVAILABLE_TITLE} message={UNAVAILABLE_MESSAGE} />
          ) : anomalies.length === 0 ? (
            <EmptyState
              title={year ? `No anomalies in ${year}` : 'No anomalies found'}
              message={year ? 'Every record is within the normal range for its facility and category.' : 'Add activity records to check for unusual values.'}
            />
          ) : (
            <div className="table-wrap" style={{ opacity: anomaliesLoading ? 0.6 : 1, transition: 'opacity .15s' }}>
              <table className="tbl">
                <thead>
                  <tr>
                    <th>Where</th>
                    <th>Period</th>
                    <th className="r">Quantity</th>
                    <th className="r">Z-score</th>
                    <th>Severity</th>
                  </tr>
                </thead>
                <tbody>
                  {anomalies.map(a => (
                    <tr className="hov" key={a.activityId}>
                      <td>
                        <span className="cell-ic">
                          <i>
                            <CategoryIcon category={a.category} />
                          </i>
                          <span>
                            <b style={{ fontWeight: 500 }}>{categoryLabel(a.category)}</b>
                            <div className="small-note">{a.facilityName}</div>
                          </span>
                        </span>
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>{formatPeriod(a.year, a.month)}</td>
                      <td className="r" style={{ whiteSpace: 'nowrap' }}>
                        {formatNumber(a.quantity)} <span className="small-note">{unitLabel(a.unit)}</span>
                        <div className="small-note">usual: about {formatNumber(a.expectedQuantity)}</div>
                      </td>
                      <td className="r">{a.zScore.toFixed(1)}</td>
                      <td>
                        <Tag kind={a.severity === 'HIGH' ? 'bad' : 'warn'}>{a.severity === 'HIGH' ? 'High' : 'Medium'}</Tag>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <Assistant />
      </div>
    </>
  )
}
