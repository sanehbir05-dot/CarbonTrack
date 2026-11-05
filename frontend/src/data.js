// Mock data for UI preview only. Replace with REST calls to the Spring Boot API later.
export const companies = ['Meridian Steel Works', 'Kaveri Textiles', 'Orion Logistics', 'Sahyadri Foods', 'Nimbus IT Services']
export const roles = ['Company Admin', 'Analyst', 'Auditor', 'Super Admin']

export const months = ['Nov', 'Dec', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct']
// monthly tCO2e by scope
export const monthly = months.map((m, i) => ({
  m,
  s1: 520 + Math.round(60 * Math.sin(i / 2)) + (i === 6 ? 210 : 0),
  s2: 1180 + Math.round(140 * Math.sin((i + 3) / 1.9)),
  s3: 640 + Math.round(70 * Math.cos(i / 2.3)),
}))

export const actualSeries = months.map((m, i) => ({ m, v: monthly[i].s1 + monthly[i].s2 + monthly[i].s3, anomaly: i === 6 }))
const last = actualSeries[actualSeries.length - 1].v
export const forecastSeries = ['Nov', 'Dec', 'Jan', 'Feb', 'Mar', 'Apr'].map((m, i) => {
  const v = Math.round(last * (1 - 0.012 * (i + 1)) + 40 * Math.sin(i))
  return { m: m + ' ’', v, hi: v + 90 + i * 25, lo: v - 90 - i * 25 }
})

export const scopeSplit = [
  { name: 'Scope 1 – Direct', v: 7100, color: '#0d0d0d' },
  { name: 'Scope 2 – Electricity', v: 14600, color: '#138a1d' },
  { name: 'Scope 3 – Value chain', v: 7900, color: '#e973a8' },
]

export const facilities = [
  { name: 'Pune Plant', region: 'Maharashtra', t: 9800, delta: -6, spark: [9, 8.8, 9.2, 8.9, 8.6, 8.4] },
  { name: 'Jamshedpur Works', region: 'Jharkhand', t: 12400, delta: 3, spark: [10, 10.4, 10.9, 11.2, 11.6, 12] },
  { name: 'Chennai Warehouse', region: 'Tamil Nadu', t: 2900, delta: -11, spark: [3.4, 3.3, 3.2, 3.1, 3, 2.9] },
  { name: 'Delhi HQ', region: 'Delhi', t: 1100, delta: -2, spark: [1.2, 1.1, 1.2, 1.1, 1.1, 1.1] },
  { name: 'Fleet (Logistics)', region: 'Pan-India', t: 3400, delta: 8, spark: [2.8, 2.9, 3, 3.1, 3.3, 3.4] },
]

export const alerts = [
  { icon: 'warn', kind: 'bad', title: 'Spike: Jamshedpur diesel use +41% vs. 3-mo avg', time: 'Today, 10:24 AM' },
  { icon: 'down', kind: 'warn', title: 'Target slip: YTD 4.2% above reduction pace', time: 'Yesterday, 3:12 PM' },
  { icon: 'check', kind: 'good', title: 'Solar rooftop at Pune reduced Scope 2 by 9%', time: 'Oct 27, 9:08 AM' },
  { icon: 'info', kind: 'info', title: 'CEA grid factor updated to v20.0 (2023-24)', time: 'Oct 26, 1:45 PM' },
]

export const activity = [
  { id: 1, fac: 'Pune Plant', cat: 'Electricity (grid)', icon: 'bolt', qty: 482000, unit: 'kWh', period: 'Oct 2026', scope: 2, co2e: 391.4, src: 'CEA v20', via: 'CSV' },
  { id: 2, fac: 'Jamshedpur Works', cat: 'Diesel – stationary', icon: 'flame', qty: 96000, unit: 'litres', period: 'Oct 2026', scope: 1, co2e: 257.5, src: 'DESNZ 2024', via: 'IoT meter' },
  { id: 3, fac: 'Fleet (Logistics)', cat: 'Road freight (HGV)', icon: 'truck', qty: 310000, unit: 'tonne-km', period: 'Oct 2026', scope: 3, co2e: 28.6, src: 'DESNZ 2024', via: 'REST API' },
  { id: 4, fac: 'Delhi HQ', cat: 'Business travel – air', icon: 'plane', qty: 84200, unit: 'passenger-km', period: 'Oct 2026', scope: 3, co2e: 14.9, src: 'DESNZ 2024', via: 'Manual' },
  { id: 5, fac: 'Chennai Warehouse', cat: 'Electricity (grid)', icon: 'bolt', qty: 121000, unit: 'kWh', period: 'Oct 2026', scope: 2, co2e: 98.2, src: 'CEA v20', via: 'IoT meter' },
  { id: 6, fac: 'Pune Plant', cat: 'Natural gas', icon: 'flame', qty: 41000, unit: 'm³', period: 'Oct 2026', scope: 1, co2e: 77.8, src: 'DESNZ 2024', via: 'Excel' },
]

export const factors = [
  { cat: 'Grid electricity – India', value: 0.82, unit: 'kg CO₂e / kWh', src: 'CEA v20', region: 'IN', year: 2023, ver: 'v3' },
  { cat: 'Diesel (stationary)', value: 2.68, unit: 'kg CO₂e / litre', src: 'DESNZ', region: 'UK/Global', year: 2024, ver: 'v2' },
  { cat: 'Natural gas', value: 1.90, unit: 'kg CO₂e / m³', src: 'DESNZ', region: 'UK/Global', year: 2024, ver: 'v2' },
  { cat: 'HGV freight (avg laden)', value: 0.092, unit: 'kg CO₂e / tonne-km', src: 'DESNZ', region: 'UK/Global', year: 2024, ver: 'v2' },
  { cat: 'Air travel – long haul, economy', value: 0.148, unit: 'kg CO₂e / pax-km', src: 'DESNZ', region: 'UK/Global', year: 2024, ver: 'v2' },
  { cat: 'Waste – landfill', value: 0.45, unit: 'kg CO₂e / kg', src: 'EPA Hub', region: 'US', year: 2023, ver: 'v1' },
]

export const actions = [
  { name: 'Rooftop solar – Pune (2.4 MW)', cat: 'Renewable energy', saving: 1850, status: 'Completed', pct: 100 },
  { name: 'EV transition – last-mile fleet', cat: 'Transport', saving: 620, status: 'In progress', pct: 55 },
  { name: 'Waste-heat recovery – Jamshedpur', cat: 'Process efficiency', saving: 1400, status: 'In progress', pct: 30 },
  { name: 'Green PPA – Chennai', cat: 'Renewable energy', saving: 900, status: 'Planned', pct: 0 },
]

export const rules = [
  { name: 'Tier A – Green Leader', cond: 'Emissions ≥ 15% below baseline', reward: '10,000 simulated carbon credits', tier: 'A', color: '#138a1d' },
  { name: 'Tier B – Green Performer', cond: 'Emissions 8–15% below baseline', reward: '4,000 simulated carbon credits', tier: 'B', color: '#f59a4d' },
  { name: 'Tier C – Improver', cond: 'Emissions 3–8% below baseline', reward: 'Eligibility letter + 1,000 credits', tier: 'C', color: '#e973a8' },
]

export const audit = [
  { who: 'anita.rao', role: 'Analyst', what: 'Edited ActivityRecord #2 quantity 88,000 → 96,000 litres', when: 'Today, 09:58' },
  { who: 'system', role: 'Scheduler', what: 'Ingested 288 IoT meter readings (Pune, Chennai)', when: 'Today, 06:00' },
  { who: 'vikram.s', role: 'Company Admin', what: 'Changed target: 2030 reduction 40% → 45%', when: 'Yesterday, 17:20' },
  { who: 'meera.k', role: 'Auditor', what: 'Approved Q2 BRSR draft', when: 'Oct 28, 11:04' },
]

export const botReplies = {
  default: 'Based on your last 12 months, Scope 2 electricity is 64% of your footprint. The biggest lever is shifting Jamshedpur to a green PPA, estimated to cut ~900 tCO₂e/yr.',
  anomaly: 'The June spike came from Jamshedpur diesel (+41%). The isolation-forest model flagged it with a score of 0.82. Likely a generator run during a grid outage — worth confirming with the facility manager.',
  target: 'You are 4.2% behind linear pace toward the 2030 target. Completing the EV fleet transition and waste-heat recovery would put you back on track by Q2.',
}
