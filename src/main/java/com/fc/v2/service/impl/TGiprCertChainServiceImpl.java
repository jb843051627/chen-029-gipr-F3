package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fc.v2.mapper.auto.TGiprCertBillMapper;
import com.fc.v2.mapper.auto.TGiprCertGrantMapper;
import com.fc.v2.mapper.auto.TGiprCertSignerMapper;
import com.fc.v2.mapper.auto.TGiprCertVoteMapper;
import com.fc.v2.mapper.auto.TGiprHolderBookMapper;
import com.fc.v2.model.auto.TGiprCertBill;
import com.fc.v2.model.auto.TGiprCertGrant;
import com.fc.v2.model.auto.TGiprCertSigner;
import com.fc.v2.model.auto.TGiprCertVote;
import com.fc.v2.model.auto.TGiprHolderBook;
import com.fc.v2.model.custom.CertBillOpenForm;
import com.fc.v2.model.custom.CertBillSnapshot;
import com.fc.v2.service.CertRuleException;
import com.fc.v2.service.ITGiprCertChainService;

/**
 * 专用标志授权签批链——一张签批单从起意到点齐怎么走完、点齐那一刻带出的授权怎么咬住，
 * 全在本类与 {@link CertChainPolicy} 一处作答。
 *
 * 段次、算齐办法、定员一律由后端关策当场数（报文自称不认）；票只认在籍签人；
 * 不可一票止住；压回抹掉本轮票从头累计；点齐即封住只读；一条企业线索同一刻只许一张在签；
 * 授权只由点齐同回带出（授权行生成 + 底册改口同一事务，一处不成整笔回滚，杜绝残留）。
 *
 * 与旧 TGiprCertBillServiceImpl 并列：旧路子形参与返回一概不动；往后按品类加关，
 * 另起一条并列的链与关策去担，莫回头改本类形状。
 *
 * @author fuce
 * @date 2026-10-07
 */
@Service
public class TGiprCertChainServiceImpl implements ITGiprCertChainService {

    @javax.annotation.Resource
    private TGiprCertBillMapper billMapper;
    @javax.annotation.Resource
    private TGiprCertVoteMapper voteMapper;
    @javax.annotation.Resource
    private TGiprCertSignerMapper signerMapper;
    @javax.annotation.Resource
    private TGiprCertGrantMapper grantMapper;
    @javax.annotation.Resource
    private TGiprHolderBookMapper holderMapper;

    private static String day(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private static String stamp(Date d) {
        return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(d);
    }

    // ================= 起意 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBillSnapshot open(CertBillOpenForm form, Date at) {
        if (form == null || at == null) {
            throw new CertRuleException("起单表单不合：没有可落的单");
        }
        if (isBlank(form.getHolderNo()) || isBlank(form.getProdNo())) {
            throw new CertRuleException("起单不合：企业代号、名录产品代号两栏都得钉齐，一笔都不入库");
        }
        // 报文本可不带人数；一旦带了，越定员量程（写 0、写 9）即整笔不合——
        // 人数这一栏不合，本关算齐办法跟着不作数
        if (form.getDeclaredNeedCount() != null
                && !CertChainPolicy.quotaInRange(form.getDeclaredNeedCount())) {
            throw new CertRuleException(CertChainPolicy.quotaMessage());
        }

        // 一条授权线索同一刻只许一张：该企业头一张还没点齐（在签/止住），第二张起不来
        if (countOpenByHolder(form.getHolderNo().trim()) > 0) {
            throw new CertRuleException("该企业头一张签批单尚未点齐，第二张起不来");
        }

        // 三关关籍按关策定员当场排：名单缺一关、关次越界、长短与定员对不上，
        // 一律指名人数这一栏不合，算齐办法不作数
        List<TGiprCertSigner> signers = rosterOf(form);

        String billNo = isBlank(form.getBillNo()) ? "CB" + stamp(at) : form.getBillNo().trim();
        if (countByBillNo(billNo) > 0) {
            throw new CertRuleException("签批单号已在册：" + billNo);
        }

        TGiprCertBill bill = new TGiprCertBill();
        bill.setBillNo(billNo);
        bill.setHolderNo(form.getHolderNo().trim());
        bill.setProdNo(form.getProdNo().trim());
        bill.setRuleCode(isBlank(form.getRuleCode()) ? null : form.getRuleCode().trim());
        CertChainPolicy.Node first = CertChainPolicy.firstNode();
        bill.setNodeNo(first.nodeNo);
        bill.setSignMode(first.mode);
        bill.setNeedCount(first.quota);
        bill.setSignCount(0);
        bill.setStatus(CertChainPolicy.STATUS_RUNNING);
        bill.setDelFlag(0);
        bill.setCreateTime(at);
        billMapper.insert(bill);

        for (TGiprCertSigner s : signers) {
            s.setBillId(bill.getId());
            s.setDelFlag(0);
            s.setCreateTime(at);
            signerMapper.insert(s);
        }
        return snapshot(bill.getId(), at);
    }

    /** 关籍当场排：三关一个不许多一个不许少，每关人数与关策定员字字相符 */
    private List<TGiprCertSigner> rosterOf(CertBillOpenForm form) {
        List<CertBillOpenForm.NodeRoster> rosters = form.getRosters();
        if (rosters == null || rosters.size() != CertChainPolicy.LAST_NODE + 1) {
            throw new CertRuleException(CertChainPolicy.quotaMessage());
        }
        List<TGiprCertSigner> out = new ArrayList<TGiprCertSigner>();
        Set<Integer> seenNodes = new HashSet<Integer>();
        for (CertBillOpenForm.NodeRoster r : rosters) {
            CertChainPolicy.Node node = r == null ? null : CertChainPolicy.nodeOf(r.getNodeNo());
            List<String> voters = r == null ? null : r.getVoterNos();
            if (node == null || voters == null || voters.size() != node.quota || !seenNodes.add(node.nodeNo)) {
                throw new CertRuleException(CertChainPolicy.quotaMessage());
            }
            Set<String> seenVoters = new HashSet<String>();
            for (int i = 0; i < voters.size(); i++) {
                String v = voters.get(i);
                if (isBlank(v) || !seenVoters.add(v.trim())) {
                    throw new CertRuleException(CertChainPolicy.quotaMessage());
                }
                TGiprCertSigner s = new TGiprCertSigner();
                s.setNodeNo(node.nodeNo);
                s.setSeatNo(i + 1);
                s.setVoterNo(v.trim());
                out.add(s);
            }
        }
        return out;
    }

    // ================= 落笔 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBillSnapshot approve(Long billId, String voterNo, String opinion, Date at) {
        return castVote(billId, voterNo, opinion, at, CertChainPolicy.VOTE_APPROVE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBillSnapshot veto(Long billId, String voterNo, String voterOpinion, Date at) {
        return castVote(billId, voterNo, voterOpinion, at, CertChainPolicy.VOTE_VETO);
    }

    private CertBillSnapshot castVote(Long billId, String voterNo, String opinion, Date at, int kind) {
        TGiprCertBill bill = requireRunningBill(billId);
        if (isBlank(voterNo)) {
            throw new CertRuleException("落笔不合：没有落款签人，这一笔不收");
        }
        int cur = bill.getNodeNo();
        // 票按签人关籍落在哪一关，不按单据自称关次
        TGiprCertSigner seat = signerMapper.selectOne(new QueryWrapper<TGiprCertSigner>()
                .eq("bill_id", bill.getId())
                .eq("voter_no", voterNo.trim())
                .eq("del_flag", 0));
        if (seat == null) {
            throw new CertRuleException("落笔不合：" + voterNo + " 不在本单在籍签人名册，这一笔不收");
        }
        int voterNode = seat.getNodeNo();
        if (voterNode > cur) {
            throw new CertRuleException("落笔不合：第 " + (voterNode + 1) + " 段还没亮出来，这一关暂不收笔");
        }

        // 同一关同一人本轮只许一票（压回后票已抹掉，可重新落）
        if (countVotesBy(bill.getId(), voterNode, seat.getVoterNo()) > 0) {
            throw new CertRuleException("落笔不合：" + seat.getVoterNo() + " 在本关本轮已落过笔，不得再落");
        }

        TGiprCertVote vote = new TGiprCertVote();
        vote.setBillId(bill.getId());
        vote.setNodeNo(voterNode);
        vote.setVoterNo(seat.getVoterNo());
        vote.setVoteKind(kind);
        vote.setOpinion(clamp(opinion));
        vote.setDelFlag(0);
        vote.setCreateTime(at);
        voteMapper.insert(vote);

        // 后到者关籍在已过的上一关：无论是「可」是「不可」，票照旧累加沉在本关行面，
        // 不作准、不触发推进、不止住当前关（先到那一方已作准）
        if (voterNode < cur) {
            return snapshot(bill.getId(), at);
        }

        if (kind == CertChainPolicy.VOTE_VETO) {
            // 不可一票止住：后头的岗不再收笔，不可的话写回本关
            bill.setStatus(CertChainPolicy.STATUS_HALTED);
            bill.setVetoOpinion(clamp(opinion));
            bill.setSignCount(countVotes(bill.getId(), cur));
            bill.setUpdateTime(at);
            billMapper.updateById(bill);
            return snapshot(bill.getId(), at);
        }

        CertChainPolicy.Node node = CertChainPolicy.nodeOf(cur);
        int approveVotes = countKind(bill.getId(), cur, CertChainPolicy.VOTE_APPROVE);
        bill.setSignCount(countVotes(bill.getId(), cur));
        if (node.completedBy(approveVotes)) {
            if (cur >= CertChainPolicy.LAST_NODE) {
                sealWithGrant(bill, at);
            } else {
                // 下一段此刻才亮出来：新关定员、算齐办法当场从关策取，已到清零从头累计
                CertChainPolicy.Node next = CertChainPolicy.nodeOf(cur + 1);
                bill.setNodeNo(next.nodeNo);
                bill.setSignMode(next.mode);
                bill.setNeedCount(next.quota);
                bill.setSignCount(0);
                bill.setStatus(CertChainPolicy.STATUS_RUNNING);
                bill.setUpdateTime(at);
                billMapper.updateById(bill);
            }
        } else {
            bill.setUpdateTime(at);
            billMapper.updateById(bill);
        }
        return snapshot(bill.getId(), at);
    }

    /** 点齐：封住 + 授权行生成 + 底册改口，出自同一回落笔；一处不成整笔回滚，不留残留 */
    private void sealWithGrant(TGiprCertBill bill, Date at) {
        CertChainPolicy.Node last = CertChainPolicy.lastNode();
        String d = day(at);
        int sameDay = countGrantsIssuedOn(d);
        int seq = sameDay + 1;

        TGiprCertGrant grant = new TGiprCertGrant();
        grant.setGrantNo("GZ" + new SimpleDateFormat("yyyyMMdd").format(at) + String.format("%03d", seq));
        grant.setBillId(bill.getId());
        grant.setHolderNo(bill.getHolderNo());
        grant.setProdNo(bill.getProdNo());
        grant.setRuleCode(bill.getRuleCode());
        grant.setFinishNode(last.nodeNo);
        grant.setIssueDate(truncateToDay(at));
        grant.setStatus(0);
        grant.setDelFlag(0);
        grant.setCreateTime(at);
        grantMapper.insert(grant);

        // 底册那一行的改口与授权那一行的生成同一回：企业行核齐、产品代号对齐、回链授权
        int changed = holderMapper.update(null, new UpdateWrapper<TGiprHolderBook>()
                .eq("holder_no", bill.getHolderNo())
                .eq("del_flag", 0)
                .set("status", 1)
                .set("prod_no", bill.getProdNo())
                .set("grant_id", grant.getId()));
        if (changed <= 0) {
            // 无册可改口即一处落不了：整笔回滚，授权行不单独留下（杜绝两处各说各话）
            throw new CertRuleException("点齐不成：底册没有该企业那一行可改口，授权与底册须同一回落笔");
        }

        bill.setGrantId(grant.getId());
        bill.setStatus(CertChainPolicy.STATUS_SEALED);
        bill.setSignCount(0);
        bill.setUpdateTime(at);
        billMapper.updateById(bill);
    }

    // ================= 压回 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertBillSnapshot pressBack(Long billId, String operator, String opinion, Date at) {
        TGiprCertBill bill = requireBill(billId);
        int status = bill.getStatus() == null ? CertChainPolicy.STATUS_RUNNING : bill.getStatus();
        if (status == CertChainPolicy.STATUS_SEALED) {
            // 点齐之后只读：压回也是动手的动作，一概不接
            throw new CertRuleException("单子已点齐封住，补笔、涂改、压回一概不接");
        }
        int cur = bill.getNodeNo();
        int target = Math.max(CertChainPolicy.FIRST_NODE, cur - 1);

        // 压回时上一轮签过的票一并抹掉不算：从 target 关到止住关本轮票全清，
        // 重新挪到这一关从头累计——旧票混着新票数作齐即为缺陷
        List<TGiprCertVote> stale = voteMapper.selectList(new QueryWrapper<TGiprCertVote>()
                .eq("bill_id", bill.getId())
                .ge("node_no", target)
                .eq("del_flag", 0));
        for (TGiprCertVote v : stale) {
            voteMapper.deleteById(v.getId());
        }

        CertChainPolicy.Node node = CertChainPolicy.nodeOf(target);
        bill.setNodeNo(node.nodeNo);
        bill.setSignMode(node.mode);
        bill.setNeedCount(node.quota);
        bill.setSignCount(0);
        bill.setStatus(CertChainPolicy.STATUS_RUNNING);
        bill.setVetoOpinion(null);
        bill.setUpdateTime(at);
        appendPressTrace(bill, operator, opinion);
        billMapper.updateById(bill);
        return snapshot(bill.getId(), at);
    }

    // ================= 回声：几个数只由这一处当场回答 =================

    @Override
    public CertBillSnapshot snapshot(Long billId, Date at) {
        TGiprCertBill bill = requireBill(billId);
        int cur = bill.getNodeNo() == null ? CertChainPolicy.FIRST_NODE : bill.getNodeNo();
        int status = bill.getStatus() == null ? CertChainPolicy.STATUS_RUNNING : bill.getStatus();
        CertChainPolicy.Node node = CertChainPolicy.nodeOf(cur);

        CertBillSnapshot s = new CertBillSnapshot();
        s.setBillId(bill.getId());
        s.setBillNo(bill.getBillNo());
        s.setHolderNo(bill.getHolderNo());
        s.setProdNo(bill.getProdNo());
        s.setRuleCode(bill.getRuleCode());
        s.setCurrentNode(cur);
        s.setCurrentNodeName(node == null ? null : node.nodeName);
        s.setSignMode(node == null ? bill.getSignMode() : node.mode);
        s.setNeedCount(node == null ? bill.getNeedCount() : node.quota);
        // 本关已到几人：当场从票行数，不读单据上任何手填的数
        s.setSignedCount(status == CertChainPolicy.STATUS_SEALED ? 0 : countVotes(bill.getId(), cur));
        s.setRemainingNodes(status == CertChainPolicy.STATUS_SEALED ? 0 : CertChainPolicy.remainingNodes(cur));
        s.setStatus(status);
        s.setStatusName(statusName(status));
        s.setSealed(status == CertChainPolicy.STATUS_SEALED);
        s.setHalted(status == CertChainPolicy.STATUS_HALTED);
        s.setVetoOpinion(status == CertChainPolicy.STATUS_HALTED ? bill.getVetoOpinion() : null);
        s.setCurrentRoundVotes(listVotes(bill.getId(), cur));
        s.setGrantId(bill.getGrantId());
        s.setSnapshotAt(at);
        if (bill.getGrantId() != null) {
            TGiprCertGrant g = grantMapper.selectById(bill.getGrantId());
            if (g != null) {
                s.setGrantNo(g.getGrantNo());
            }
        }
        return s;
    }

    // ================= 年底复核：授权行回查底册行，字字相等才咬得住 =================

    @Override
    public boolean verifyGrantLedger(Long grantId) {
        if (grantId == null) {
            return false;
        }
        TGiprCertGrant g = grantMapper.selectById(grantId);
        if (g == null || (g.getDelFlag() != null && g.getDelFlag() != 0)) {
            return false;
        }
        TGiprCertBill b = billMapper.selectById(g.getBillId());
        if (b == null || (b.getDelFlag() != null && b.getDelFlag() != 0)) {
            return false;
        }
        // 一单一授：授权行只能被带出它的那一张回指，邻张拿来不认
        if (!Objects.equals(b.getGrantId(), g.getId()) || countByGrantId(g.getId()) != 1) {
            return false;
        }
        // 四样钉死、证面关次与走完当场关次字字相等；改线之后重算判不符
        if (b.getStatus() == null || b.getStatus() != CertChainPolicy.STATUS_SEALED) {
            return false;
        }
        if (!Objects.equals(b.getHolderNo(), g.getHolderNo())
                || !Objects.equals(b.getProdNo(), g.getProdNo())
                || !Objects.equals(b.getRuleCode(), g.getRuleCode())) {
            return false;
        }
        if (g.getFinishNode() == null || g.getFinishNode() != CertChainPolicy.LAST_NODE
                || !Objects.equals(g.getFinishNode(), b.getNodeNo())) {
            return false;
        }
        // 核发当日那一个数：字头与核发当日字字相等，尾数为当日三位顺序号
        if (isBlank(g.getGrantNo()) || g.getIssueDate() == null
                || !g.getGrantNo().startsWith("GZ" + new SimpleDateFormat("yyyyMMdd").format(g.getIssueDate()))
                || !g.getGrantNo().substring(10).matches("\\d{3}")) {
            return false;
        }
        // 底册那一行的改口须与授权同一回：凭授权回查底册，企业、产品、回链三处对齐
        TGiprHolderBook h = holderMapper.selectOne(new QueryWrapper<TGiprHolderBook>()
                .eq("holder_no", g.getHolderNo())
                .eq("grant_id", g.getId())
                .eq("del_flag", 0));
        return h != null && Objects.equals(h.getProdNo(), g.getProdNo());
    }

    // ================= 内学 =================

    private TGiprCertBill requireBill(Long billId) {
        if (billId == null) {
            throw new CertRuleException("落笔不合：没有这一张单");
        }
        TGiprCertBill bill = billMapper.selectById(billId);
        if (bill == null || (bill.getDelFlag() != null && bill.getDelFlag() != 0)) {
            throw new CertRuleException("落笔不合：册里没有这一张单");
        }
        return bill;
    }

    /** 在签才收笔：已点齐封住（补笔/追签/涂改三不接）、已止住（先压回）都不收 */
    private TGiprCertBill requireRunningBill(Long billId) {
        TGiprCertBill bill = requireBill(billId);
        int status = bill.getStatus() == null ? CertChainPolicy.STATUS_RUNNING : bill.getStatus();
        if (status == CertChainPolicy.STATUS_SEALED) {
            throw new CertRuleException("单子已点齐封住：再补一笔、涂改先前一句、事后追签三种动作一概不接");
        }
        if (status == CertChainPolicy.STATUS_HALTED) {
            throw new CertRuleException("单子已被不可一票止住：后头的岗不再收笔，要缓过来只能压回上一关重签");
        }
        return bill;
    }

    private int countVotes(Long billId, int node) {
        Integer n = voteMapper.selectCount(new QueryWrapper<TGiprCertVote>()
                .eq("bill_id", billId).eq("node_no", node).eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private int countVotesBy(Long billId, int node, String voterNo) {
        Integer n = voteMapper.selectCount(new QueryWrapper<TGiprCertVote>()
                .eq("bill_id", billId).eq("node_no", node)
                .eq("voter_no", voterNo).eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private int countKind(Long billId, int node, int kind) {
        Integer n = voteMapper.selectCount(new QueryWrapper<TGiprCertVote>()
                .eq("bill_id", billId).eq("node_no", node)
                .eq("vote_kind", kind).eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private List<TGiprCertVote> listVotes(Long billId, int node) {
        return voteMapper.selectList(new QueryWrapper<TGiprCertVote>()
                .eq("bill_id", billId).eq("node_no", node).eq("del_flag", 0)
                .orderByAsc("create_time").orderByAsc("id"));
    }

    /** 在签线索：未点齐（在签 0 / 止住 2）都算头一张还没走完，第二张起不来 */
    private int countOpenByHolder(String holderNo) {
        Integer n = billMapper.selectCount(new QueryWrapper<TGiprCertBill>()
                .eq("holder_no", holderNo)
                .in("status", CertChainPolicy.STATUS_RUNNING, CertChainPolicy.STATUS_HALTED)
                .eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private int countByBillNo(String billNo) {
        Integer n = billMapper.selectCount(new QueryWrapper<TGiprCertBill>()
                .eq("bill_no", billNo).eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private int countByGrantId(Long grantId) {
        Integer n = billMapper.selectCount(new QueryWrapper<TGiprCertBill>()
                .eq("grant_id", grantId).eq("del_flag", 0));
        return n == null ? 0 : n;
    }

    private int countGrantsIssuedOn(String d) {
        Integer n = grantMapper.selectCount(new QueryWrapper<TGiprCertGrant>()
                .eq("del_flag", 0)
                .ge("issue_date", d)
                .lt("issue_date", nextDay(d)));
        return n == null ? 0 : n;
    }

    private static String statusName(int status) {
        if (status == CertChainPolicy.STATUS_SEALED) {
            return "已点齐封住";
        }
        if (status == CertChainPolicy.STATUS_HALTED) {
            return "已止住";
        }
        return "在签";
    }

    private static Date truncateToDay(Date at) {
        Calendar c = Calendar.getInstance();
        c.setTime(at);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static String nextDay(String d) {
        try {
            Date parsed = new SimpleDateFormat("yyyy-MM-dd").parse(d);
            Calendar c = Calendar.getInstance();
            c.setTime(parsed);
            c.add(Calendar.DAY_OF_YEAR, 1);
            return new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        } catch (java.text.ParseException e) {
            throw new CertRuleException("核发当日口径不合：" + d);
        }
    }

    private static String clamp(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() > 500 ? t.substring(t.length() - 500) : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void appendPressTrace(TGiprCertBill bill, String operator, String opinion) {
        StringBuilder trace = new StringBuilder();
        if (bill.getRemark() != null && !bill.getRemark().isEmpty()) {
            trace.append(bill.getRemark()).append(';');
        }
        trace.append("压回").append(bill.getNodeNo() + 1).append("段重签");
        if (!isBlank(operator)) {
            trace.append('[').append(operator.trim()).append(']');
        }
        if (!isBlank(opinion)) {
            trace.append(':').append(opinion.trim());
        }
        String t = trace.toString();
        if (t.length() > 500) {
            t = t.substring(t.length() - 500);
        }
        bill.setRemark(t);
    }
}
