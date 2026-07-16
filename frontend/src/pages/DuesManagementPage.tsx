import { useEffect, useState, type FormEvent } from "react";
import { useParams } from "react-router-dom";
import { AppLayout } from "../components/AppLayout";
import { listGenerations } from "../api/generations";
import { apiErrorMessage } from "../api/http";
import { cancelDuesPayment, cancelDuesRefund, changeDuesExemption, createDuesPolicy, getDuesOverview, getDuesRefundQuote, recordDuesPayment, recordDuesRefund } from "../api/dues";
import type { Generation } from "../types/generation";
import type { DuesOverview, DuesRefundQuote } from "../types/dues";

type RuleForm = { label: string; endsOn: string; ratePercent: string };
const emptyOverview: DuesOverview = { totalAssessed: "0", totalPaid: "0", totalRefunded: "0", unpaidCount: 0, members: [], refundRules: [] };
const statusLabel: Record<string, string> = { UNPAID: "미납", PAID: "납부", EXEMPT: "면제", REFUNDED: "전액 환불", PARTIALLY_REFUNDED: "일부 환불", REFUND_NOT_APPLICABLE: "환불 불가" };
const memberStatusLabel: Record<string, string> = { REGULAR: "회원", ASSOCIATE: "준회원", INACTIVE: "비활동", WITHDRAWN: "탈퇴" };

function money(value?: string) {
  if (!value) return "0원";
  try { return `${BigInt(value).toLocaleString("ko-KR")}원`; } catch { return `${value}원`; }
}
function today() { return new Date().toISOString().slice(0, 10); }

export function DuesManagementPage() {
  const { clubId = "" } = useParams();
  const [generations, setGenerations] = useState<Generation[]>([]);
  const [generationId, setGenerationId] = useState("");
  const [overview, setOverview] = useState<DuesOverview>(emptyOverview);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submittingId, setSubmittingId] = useState("");
  const [amount, setAmount] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [rules, setRules] = useState<RuleForm[]>([{ label: "OT 전 50% 환불", endsOn: "", ratePercent: "50" }]);
  const [refundQuote, setRefundQuote] = useState<{ dueId: string; quote: DuesRefundQuote } | null>(null);

  useEffect(() => {
    let cancelled = false;
    listGenerations(clubId).then(items => {
      if (cancelled) return;
      setGenerations(items);
      setGenerationId(items.find(item => item.status === "ACTIVE")?.id ?? items[0]?.id ?? "");
      if (items.length === 0) setLoading(false);
    }).catch(err => setError(apiErrorMessage(err, "학기를 불러오지 못했습니다.")));
    return () => { cancelled = true; };
  }, [clubId]);

  useEffect(() => {
    if (!generationId) return;
    let cancelled = false;
    Promise.resolve().then(() => {
      if (!cancelled) { setLoading(true); setError(""); }
    });
    getDuesOverview(clubId, generationId).then(data => { if (!cancelled) setOverview(data); })
      .catch(err => { if (!cancelled) setError(apiErrorMessage(err, "회비 정보를 불러오지 못했습니다.")); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [clubId, generationId]);

  async function handleCreatePolicy(event: FormEvent) {
    event.preventDefault(); setError(""); setSubmittingId("policy");
    if (!/^[0-9]{1,19}$/.test(amount) || amount === "0") { setError("회비를 원 단위 숫자로 입력해 주세요."); setSubmittingId(""); return; }
    if (rules.some(rule => !rule.label.trim() || !rule.endsOn || !/^\d{1,3}$/.test(rule.ratePercent) || Number(rule.ratePercent) > 100)) {
      setError("환불 기준 이름·마감일·0~100% 비율을 확인해 주세요."); setSubmittingId(""); return;
    }
    try {
      setOverview(await createDuesPolicy(clubId, generationId, { amount, ...(dueDate ? { dueDate } : {}), refundRules: rules.map(rule => ({ label: rule.label.trim(), endsOn: rule.endsOn, refundRateBps: Number(rule.ratePercent) * 100 })) }));
    } catch (err) { setError(apiErrorMessage(err, "회비를 설정하지 못했습니다.")); }
    finally { setSubmittingId(""); }
  }

  async function run(dueId: string, action: () => Promise<DuesOverview>) {
    setSubmittingId(dueId); setError("");
    try { setOverview(await action()); } catch (err) { setError(apiErrorMessage(err, "회비 정보를 변경하지 못했습니다.")); }
    finally { setSubmittingId(""); }
  }

  async function quoteRefund(dueId: string) {
    setSubmittingId(dueId); setError("");
    try { setRefundQuote({ dueId, quote: await getDuesRefundQuote(dueId) }); }
    catch (err) { setError(apiErrorMessage(err, "환불액을 계산하지 못했습니다.")); }
    finally { setSubmittingId(""); }
  }

  return <AppLayout clubId={clubId}>
    <header className="border-b border-[var(--border-subtle)] bg-white px-4 py-5 md:px-8">
      <h1 className="text-xl font-extrabold">회비 관리</h1><p className="mt-1 text-xs text-[var(--text-secondary)]">학기별 회비 납부와 환불을 관리합니다.</p>
    </header>
    <main className="p-4 md:p-8">
      <label className="mb-5 grid max-w-sm gap-1.5 text-xs font-bold">조회할 학기
        <select className="control" value={generationId} onChange={event => setGenerationId(event.target.value)}>{generations.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
      </label>
      {error && <p role="alert" className="mb-4 rounded-lg bg-[var(--danger-soft)] p-3 text-xs font-bold text-[var(--danger)]">{error}</p>}
      {loading ? <p className="text-sm text-[var(--text-secondary)]">불러오는 중...</p> : !overview.policyId ?
        <form onSubmit={handleCreatePolicy} className="max-w-2xl rounded-xl border border-[var(--border-subtle)] bg-white p-5">
          <h2 className="font-extrabold">학기 회비 설정</h2>
          <div className="mt-4 grid gap-3 sm:grid-cols-2"><label className="grid gap-1 text-xs font-bold">회비(원)<input className="control" inputMode="numeric" value={amount} onChange={e => setAmount(e.target.value)} placeholder="30000" /></label><label className="grid gap-1 text-xs font-bold">납부 기한<input className="control" type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} /></label></div>
          <h3 className="mt-5 text-sm font-extrabold">환불 기준</h3><p className="mt-1 text-xs text-[var(--text-secondary)]">마지막 기준일 이후는 자동으로 환불 불가입니다.</p>
          <div className="mt-3 grid gap-2">{rules.map((rule, index) => <div key={index} className="grid gap-2 rounded-lg bg-[var(--panel-muted)] p-3 sm:grid-cols-[1fr_150px_90px_auto]"><input aria-label={`환불 기준 ${index + 1} 이름`} className="control" value={rule.label} onChange={e => setRules(current => current.map((item, i) => i === index ? { ...item, label: e.target.value } : item))} /><input aria-label={`환불 기준 ${index + 1} 마감일`} className="control" type="date" value={rule.endsOn} onChange={e => setRules(current => current.map((item, i) => i === index ? { ...item, endsOn: e.target.value } : item))} /><input aria-label={`환불 기준 ${index + 1} 비율`} className="control" inputMode="numeric" value={rule.ratePercent} onChange={e => setRules(current => current.map((item, i) => i === index ? { ...item, ratePercent: e.target.value } : item))} /><button type="button" onClick={() => setRules(current => current.filter((_, i) => i !== index))} className="text-xs font-bold">삭제</button></div>)}</div>
          <div className="mt-3 flex justify-between"><button type="button" onClick={() => setRules(current => [...current, { label: "", endsOn: "", ratePercent: "" }])} className="text-xs font-bold underline">기준 추가</button><button disabled={submittingId === "policy"} className="rounded-lg bg-[var(--navy)] px-4 py-2.5 text-xs font-bold text-white disabled:opacity-50">{submittingId === "policy" ? "설정 중..." : "회비 설정"}</button></div>
        </form> : <>
          <section className="grid gap-3 sm:grid-cols-4">{[["학기 회비", overview.amount], ["총 부과", overview.totalAssessed], ["총 납부", overview.totalPaid], ["총 환불", overview.totalRefunded]].map(([label, value]) => <div key={label} className="rounded-xl border border-[var(--border-subtle)] bg-white p-4"><p className="text-xs text-[var(--text-secondary)]">{label}</p><p className="mt-1 text-lg font-extrabold">{money(value)}</p></div>)}</section>
          <p className="mt-3 text-xs font-bold text-[var(--text-secondary)]">미납 {overview.unpaidCount ?? 0}명 · 납부 기한 {overview.dueDate ?? "미설정"}</p>
          <div className="mt-5 overflow-x-auto rounded-xl border border-[var(--border-subtle)] bg-white">
            <table className="w-full min-w-[900px] text-left text-xs">
              <thead className="bg-[var(--panel-muted)]"><tr>{["이름", "구분", "부과", "납부", "환불", "상태", "처리"].map(item => <th key={item} className="px-4 py-3">{item}</th>)}</tr></thead>
              <tbody>{overview.members?.map(row => (
                <tr key={row.memberDueId} className="border-t border-[var(--border-subtle)]">
                  <td className="px-4 py-3 font-bold">{row.name}<span className="ml-2 text-[10px] text-[var(--text-secondary)]">{row.studentNumber}</span></td>
                  <td className="px-4 py-3">{memberStatusLabel[row.memberStatus ?? ""]}</td>
                  <td className="px-4 py-3">{money(row.assessedAmount)}</td>
                  <td className="px-4 py-3">{money(row.paidAmount)}{row.legacyPayment && <span className="block text-[10px] text-[var(--warning)]">기존 상태 이관</span>}</td>
                  <td className="px-4 py-3">{money(row.refundedAmount)}</td>
                  <td className="px-4 py-3 font-bold">{statusLabel[row.status ?? ""]}</td>
                  <td className="px-4 py-3"><div className="flex flex-wrap gap-2">
                    {row.status === "UNPAID" && <>
                      <button disabled={submittingId === row.memberDueId} onClick={() => void run(row.memberDueId!, () => recordDuesPayment(row.memberDueId!, today(), crypto.randomUUID()))} className="rounded bg-[var(--navy)] px-2 py-1.5 text-white">납부 확인</button>
                      <button disabled={submittingId === row.memberDueId} onClick={() => { const reason = window.prompt("면제 사유를 입력해 주세요."); if (reason) void run(row.memberDueId!, () => changeDuesExemption(row.memberDueId!, true, reason)); }} className="rounded border px-2 py-1.5">면제</button>
                    </>}
                    {row.status === "EXEMPT" && <button disabled={submittingId === row.memberDueId} onClick={() => void run(row.memberDueId!, () => changeDuesExemption(row.memberDueId!, false))} className="rounded border px-2 py-1.5">면제 해제</button>}
                    {row.memberStatus === "WITHDRAWN" && row.status === "PAID" && <button onClick={() => void quoteRefund(row.memberDueId!)} className="rounded border px-2 py-1.5">환불 계산</button>}
                    {row.status === "PAID" && <button onClick={() => { const reason = window.prompt("납부 취소 사유를 입력해 주세요."); if (reason) void run(row.memberDueId!, () => cancelDuesPayment(row.memberDueId!, reason)); }} className="rounded border px-2 py-1.5">납부 취소</button>}
                    {["REFUNDED", "PARTIALLY_REFUNDED", "REFUND_NOT_APPLICABLE"].includes(row.status ?? "") && <button onClick={() => { const reason = window.prompt("환불 기록 취소 사유를 입력해 주세요."); if (reason) void run(row.memberDueId!, () => cancelDuesRefund(row.memberDueId!, reason)); }} className="rounded border px-2 py-1.5">환불 기록 취소</button>}
                  </div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
          {refundQuote && <section className="mt-4 rounded-xl border border-[var(--warning)] bg-[var(--warning-soft)] p-4">
            <p className="text-sm font-extrabold">예상 환불액 {money(refundQuote.quote.refundAmount)}</p>
            <p className="mt-1 text-xs">탈퇴일 {refundQuote.quote.withdrawalDate} · {refundQuote.quote.ruleLabel} · {(refundQuote.quote.refundRateBps ?? 0) / 100}% · 원 미만 버림</p>
            <div className="mt-3 flex justify-end gap-2"><button onClick={() => setRefundQuote(null)} className="px-3 py-2 text-xs font-bold">취소</button><button onClick={() => void run(refundQuote.dueId, async () => { const next = await recordDuesRefund(refundQuote.dueId, crypto.randomUUID()); setRefundQuote(null); return next; })} className="rounded-lg bg-[var(--navy)] px-3 py-2 text-xs font-bold text-white">실제 송금 후 환불 완료 기록</button></div>
          </section>}
        </>}
    </main>
  </AppLayout>;
}
