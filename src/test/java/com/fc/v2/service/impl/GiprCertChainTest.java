package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
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

/**
 * 签批链回归：三段算齐、不可止住、压回抹票、点齐封住、单线在签、授权同回带出与年底回查。
 * mapper 全部打桩、纯内存验，不连库。
 */
class GiprCertChainTest {

    private static final Date AT;
    static {
        try {
            AT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2026-10-07 10:00:00");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TGiprCertBillMapper billMapper;
    private TGiprCertVoteMapper voteMapper;
    private TGiprCertSignerMapper signerMapper;
    private TGiprCertGrantMapper grantMapper;
    private TGiprHolderBookMapper holderMapper;
    private TGiprCertChainServiceImpl svc;

    private final Map<Long, TGiprCertBill> bills = new HashMap<Long, TGiprCertBill>();
    private final Map<Long, TGiprCertVote> votes = new HashMap<Long, TGiprCertVote>();
    private final Map<Long, TGiprCertSigner> signers = new HashMap<Long, TGiprCertSigner>();
    private final Map<Long, TGiprCertGrant> grants = new HashMap<Long, TGiprCertGrant>();
    private final Map<String, TGiprHolderBook> holders = new HashMap<String, TGiprHolderBook>();
    private final AtomicLong seq = new AtomicLong(1000);
    private int holderUpdateResult = 1;

    @BeforeEach
    void setUp() {
        billMapper = mock(TGiprCertBillMapper.class);
        voteMapper = mock(TGiprCertVoteMapper.class);
        signerMapper = mock(TGiprCertSignerMapper.class);
        grantMapper = mock(TGiprCertGrantMapper.class);
        holderMapper = mock(TGiprHolderBookMapper.class);
        svc = new TGiprCertChainServiceImpl();
        ReflectionTestUtils.setField(svc, "billMapper", billMapper);
        ReflectionTestUtils.setField(svc, "voteMapper", voteMapper);
        ReflectionTestUtils.setField(svc, "signerMapper", signerMapper);
        ReflectionTestUtils.setField(svc, "grantMapper", grantMapper);
        ReflectionTestUtils.setField(svc, "holderMapper", holderMapper);
        wireBill();
        wireVote();
        wireSigner();
        wireGrant();
        wireHolder();
    }

    // ================= 起意 =================

    @Test
    void openLaysRosterByBackendPolicyNotPayload() {
        CertBillSnapshot s = svc.open(validForm("H1", "GP00", null), AT);
        assertEquals(0, s.getCurrentNode().intValue());
        assertEquals("县级初核", s.getCurrentNodeName());
        assertEquals(0, s.getSignMode().intValue());
        assertEquals(1, s.getNeedCount().intValue());
        assertEquals(0, s.getSignedCount().intValue());
        assertEquals(2, s.getRemainingNodes().intValue());
        assertEquals("在签", s.getStatusName());
        // 三关关籍 1+2+2=5，全由关策定员当场排
        assertEquals(5, signers.size());
        // 报文自称的关次/模式没有任何入口写进单据
        TGiprCertBill bill = bills.get(s.getBillId());
        assertEquals(0, bill.getNodeNo().intValue());
        assertEquals(0, bill.getSignMode().intValue());
        assertEquals(1, bill.getNeedCount().intValue());
    }

    @Test
    void openRejectsOutOfQuotaDeclaredCount() {
        for (Integer bad : Arrays.asList(0, 9)) {
            CertBillOpenForm f = validForm("H" + bad, "GP00", null);
            f.setDeclaredNeedCount(bad);
            CertRuleException e = assertThrows(CertRuleException.class, () -> svc.open(f, AT));
            assertTrue(e.getMessage().contains("应签人数这一栏不合"), e.getMessage());
            assertTrue(e.getMessage().contains("算齐办法"), e.getMessage());
        }
        verify(billMapper, never()).insert(any());
    }

    @Test
    void openRejectsRosterMismatchAndStageOutOfRange() {
        CertBillOpenForm f = validForm("H2", "GP00", null);
        // 省局只给 1 人，与定员 2 对不上：人数栏不合
        f.getRosters().get(2).setVoterNos(Arrays.asList("p1"));
        assertThrows(CertRuleException.class, () -> svc.open(f, AT));

        CertBillOpenForm g = validForm("H3", "GP00", null);
        g.getRosters().get(2).setNodeNo(7);
        assertThrows(CertRuleException.class, () -> svc.open(g, AT));

        CertBillOpenForm h = validForm("H4", "GP00", null);
        h.setRosters(h.getRosters().subList(0, 2));
        assertThrows(CertRuleException.class, () -> svc.open(h, AT));
        verify(billMapper, never()).insert(any());
    }

    @Test
    void secondBillCannotOpenWhileFirstUnfinished() {
        svc.open(validForm("H9", "GP00", null), AT);
        // 头一张在签：第二张起不来
        CertRuleException e = assertThrows(CertRuleException.class,
                () -> svc.open(validForm("H9", "GP01", null), AT));
        assertTrue(e.getMessage().contains("第二张起不来"));
        assertEquals(1, bills.size());
    }

    // ================= 三段算齐 =================

    @Test
    void countyOneSignatureCompletesAndCityOpens() {
        Long id = svc.open(validForm("H1", "GP00", null), AT).getBillId();
        CertBillSnapshot s = svc.approve(id, "c1", "齐", AT);
        assertEquals(1, s.getCurrentNode().intValue());
        assertEquals("市级核认", s.getCurrentNodeName());
        assertEquals(2, s.getNeedCount().intValue());
        assertEquals(0, s.getSignedCount().intValue());
        assertEquals(1, s.getRemainingNodes().intValue());
    }

    @Test
    void cityFirstApprovalDecidesLateVotesSink() {
        Long id = openToCity();
        CertBillSnapshot s = svc.approve(id, "m1", "先到", AT);
        // 先到那一方作准：进省局
        assertEquals(2, s.getCurrentNode().intValue());
        assertEquals(0, s.getSignedCount().intValue());

        // 后到者的「不可」也照旧沉在市关行面：作不了准、不止住当前关
        CertBillSnapshot late = svc.veto(id, "m2", "有意见", AT);
        assertEquals(2, late.getCurrentNode().intValue());
        assertEquals(0, late.getSignedCount().intValue());
        assertEquals(0, late.getStatus().intValue());
        // 市关行面沉着两笔：先到的「可」与后到的「不可」
        assertEquals(2, countNodeVotes(id, 1));

        // 同一人本关本轮不得再落第二笔
        assertThrows(CertRuleException.class, () -> svc.approve(id, "m2", "改口可", AT));
        // 省局在籍人照常落笔，后到沉底的「不可」没有止住这张单
        CertBillSnapshot p1 = svc.approve(id, "p1", "可", AT);
        assertEquals(2, p1.getCurrentNode().intValue());
        assertEquals(1, p1.getSignedCount().intValue());
    }

    @Test
    void provinceRequiresTwoSameApprovals() {
        Long id = openToProvince();
        CertBillSnapshot one = svc.approve(id, "p1", "可", AT);
        assertEquals(2, one.getCurrentNode().intValue());
        assertEquals(1, one.getSignedCount().intValue());
        assertEquals(0, one.getStatus().intValue());

        CertBillSnapshot two = svc.approve(id, "p2", "可", AT);
        assertEquals(1, two.getStatus().intValue());
        assertTrue(two.isSealed());
        assertNotNull(two.getGrantId());
    }

    @Test
    void provinceDivergenceHaltsAndWritesOpinionBack() {
        Long id = openToProvince();
        svc.approve(id, "p1", "可", AT);
        CertBillSnapshot halt = svc.veto(id, "p2", "材料存疑", AT);
        assertEquals(2, halt.getStatus().intValue());
        assertTrue(halt.isHalted());
        assertEquals("材料存疑", halt.getVetoOpinion());
        assertEquals(2, halt.getCurrentRoundVotes().size());

        // 止住：后头的岗不再收笔
        assertThrows(CertRuleException.class, () -> svc.approve(id, "p2", "改主意可", AT));
        assertThrows(CertRuleException.class, () -> svc.veto(id, "p1", "不可", AT));
    }

    @Test
    void unknownAndFutureAndDuplicateVotersRejected() {
        Long id = openToCity();
        assertThrows(CertRuleException.class, () -> svc.approve(id, "outsider", "x", AT));
        // 省局还没亮出来
        assertThrows(CertRuleException.class, () -> svc.approve(id, "p1", "x", AT));
        svc.approve(id, "m1", "x", AT);
        // m1 已作准进省局，同一人在市关本轮不能再投
        assertThrows(CertRuleException.class, () -> svc.approve(id, "m1", "y", AT));
    }

    // ================= 压回：抹掉上一轮，从头累计 =================

    @Test
    void pressBackClearsRoundVotesAndRestartsCount() throws Exception {
        Long id = openToProvince();
        svc.approve(id, "p1", "可", AT);
        svc.veto(id, "p2", "存疑", AT);

        CertBillSnapshot back = svc.pressBack(id, "chief", "重核", AT);
        assertEquals(1, back.getCurrentNode().intValue());
        assertEquals(0, back.getSignedCount().intValue());
        assertEquals(0, back.getStatus().intValue());
        assertNull(back.getVetoOpinion());
        // 市关、省关本轮票全抹；县关票也因重回市关前链而保留在 0 关
        assertEquals(0, countNodeVotes(id, 1));
        assertEquals(0, countNodeVotes(id, 2));
        assertEquals(1, countNodeVotes(id, 0));

        // 旧票抹掉：m1 可重新落笔，从头累计再次进省局
        CertBillSnapshot again = svc.approve(id, "m1", "重新可", AT);
        assertEquals(2, again.getCurrentNode().intValue());
    }

    @Test
    void pressBackAtFirstNodeStaysFirstAndClearsItsVotes() {
        Long id = svc.open(validForm("H1", "GP00", null), AT).getBillId();
        svc.approve(id, "c1", "可", AT); // 已进市关
        // 连续压回：省局无人受理时从市关压回县关
        svc.pressBack(id, "chief", null, AT);
        CertBillSnapshot b0 = svc.pressBack(id, "chief", null, AT);
        assertEquals(0, b0.getCurrentNode().intValue());
        assertEquals(0, b0.getSignedCount().intValue());
    }

    // ================= 点齐：封住只读 + 授权同回带出 =================

    @Test
    void sealedBillRejectsAnyFurtherAction() {
        Long id = sealHappyBill();
        assertThrows(CertRuleException.class, () -> svc.approve(id, "p1", "补一笔", AT));
        assertThrows(CertRuleException.class, () -> svc.veto(id, "p2", "翻案", AT));
        assertThrows(CertRuleException.class, () -> svc.pressBack(id, "chief", "退回", AT));
        CertBillSnapshot s = svc.snapshot(id, AT);
        assertEquals(0, s.getSignedCount().intValue());
        assertEquals(0, s.getRemainingNodes().intValue());
    }

    @Test
    void grantIsBornInSameStrokeWithPinnedFields() {
        Long id = openToProvince("H7", "GP00", "RULE-A");
        svc.approve(id, "p1", "可", AT);
        svc.approve(id, "p2", "可", AT);

        assertEquals(1, grants.size());
        TGiprCertGrant g = grants.values().iterator().next();
        assertEquals("GZ20261007001", g.getGrantNo());
        assertEquals(id, g.getBillId());
        assertEquals("H7", g.getHolderNo());
        assertEquals("GP00", g.getProdNo());
        assertEquals("RULE-A", g.getRuleCode());
        assertEquals(2, g.getFinishNode().intValue());
        // 底册同回改口、回链授权
        TGiprHolderBook h = holders.get("H7");
        assertEquals(1, h.getStatus().intValue());
        assertEquals("GP00", h.getProdNo());
        assertEquals(g.getId(), h.getGrantId());
        assertEquals(g.getId(), bills.get(id).getGrantId());
    }

    @Test
    void grantNumberCountsSameDaySequence() {
        // 当日已发过一张：本张证面尾数当场顺到 002
        TGiprCertGrant earlier = new TGiprCertGrant();
        earlier.setId(5001L);
        earlier.setIssueDate(AT);
        earlier.setDelFlag(0);
        grants.put(5001L, earlier);

        Long id = openToProvince();
        svc.approve(id, "p1", "可", AT);
        svc.approve(id, "p2", "可", AT);
        TGiprCertGrant g = grants.get(bills.get(id).getGrantId());
        assertEquals("GZ20261007002", g.getGrantNo());
    }

    @Test
    void ledgerChangeFailureRollsBackWholeStroke() throws Exception {
        holderUpdateResult = 0;
        Long id = openToProvince();
        svc.approve(id, "p1", "可", AT);
        // 授权插入先发生、底册改口落空：同一回里一处不成，整笔判失败（事务回滚，不留残留）
        assertThrows(CertRuleException.class, () -> svc.approve(id, "p2", "可", AT));
        TGiprCertBill bill = bills.get(id);
        // 单据未被点齐；生产环境 @Transactional 把已插入的授权行一并回滚
        assertNull(bill.getGrantId());
        assertEquals(0, bill.getStatus().intValue());
        assertTrue(TGiprCertChainServiceImpl.class
                .getMethod("approve", Long.class, String.class, String.class, Date.class)
                .isAnnotationPresent(Transactional.class));
    }

    // ================= 年底复核：授权行回查底册行 =================

    @Test
    void verifyHappyGrant() {
        Long grantId = seedConsistentGrant("H10", "GP00", "RULE-A", "GZ20261007001");
        assertTrue(svc.verifyGrantLedger(grantId));
    }

    @Test
    void verifyRejectsLineChangedAfterSeal() {
        Long grantId = seedConsistentGrant("H11", "GP00", "RULE-A", "GZ20261007001");
        // 改线之后重算：授权行上的线与带出它的单不再字字相等
        grants.get(grantId).setRuleCode("RULE-B");
        assertFalse(svc.verifyGrantLedger(grantId));
    }

    @Test
    void verifyRejectsGrantBorrowedFromNeighborBill() {
        Long grantId = seedConsistentGrant("H12", "GP00", "RULE-A", "GZ20261007001");
        // 邻张拿来：授权回指的单不认这一条
        TGiprCertBill neighbor = new TGiprCertBill();
        neighbor.setId(9001L);
        neighbor.setDelFlag(0);
        neighbor.setStatus(1);
        neighbor.setGrantId(8001L);
        neighbor.setNodeNo(2);
        neighbor.setHolderNo("H12");
        neighbor.setProdNo("GP00");
        neighbor.setRuleCode("RULE-A");
        bills.put(9001L, neighbor);
        grants.get(grantId).setBillId(9001L);
        assertFalse(svc.verifyGrantLedger(grantId));
    }

    @Test
    void verifyRejectsLedgerMismatchAndWrongIssueNo() {
        Long grantId = seedConsistentGrant("H13", "GP00", "RULE-A", "GZ20261007001");
        // 底册改口那行的产品对不上
        holders.get("H13").setProdNo("GP99");
        assertFalse(svc.verifyGrantLedger(grantId));
        holders.get("H13").setProdNo("GP00");

        // 证面数与核发当日不字字相等
        grants.get(grantId).setGrantNo("GZ20261006001");
        assertFalse(svc.verifyGrantLedger(grantId));
    }

    // ================= 帮手 =================

    private CertBillOpenForm validForm(String holder, String prod, String rule) {
        CertBillOpenForm f = new CertBillOpenForm();
        f.setHolderNo(holder);
        f.setProdNo(prod);
        f.setRuleCode(rule);
        f.setRosters(Arrays.asList(
                roster(0, "c1"),
                roster(1, "m1", "m2"),
                roster(2, "p1", "p2")));
        return f;
    }

    private static CertBillOpenForm.NodeRoster roster(int node, String... voters) {
        CertBillOpenForm.NodeRoster r = new CertBillOpenForm.NodeRoster();
        r.setNodeNo(node);
        r.setVoterNos(new ArrayList<String>(Arrays.asList(voters)));
        return r;
    }

    private Long openToCity() {
        Long id = svc.open(validForm("H1", "GP00", null), AT).getBillId();
        svc.approve(id, "c1", "可", AT);
        return id;
    }

    private Long openToProvince() {
        return openToProvince("H1", "GP00", null);
    }

    private Long openToProvince(String holder, String prod, String rule) {
        Long id = svc.open(validForm(holder, prod, rule), AT).getBillId();
        svc.approve(id, "c1", "可", AT);
        svc.approve(id, "m1", "可", AT);
        return id;
    }

    private Long sealHappyBill() {
        Long id = openToProvince();
        svc.approve(id, "p1", "可", AT);
        svc.approve(id, "p2", "可", AT);
        return id;
    }

    private int countNodeVotes(Long billId, int node) {
        int n = 0;
        for (TGiprCertVote v : votes.values()) {
            if (billId.equals(v.getBillId()) && v.getNodeNo() == node
                    && (v.getDelFlag() == null || v.getDelFlag() == 0)) {
                n++;
            }
        }
        return n;
    }

    private Long seedConsistentGrant(String holder, String prod, String rule, String grantNo) {
        TGiprCertBill bill = new TGiprCertBill();
        bill.setId(seq.incrementAndGet());
        bill.setBillNo("CB-" + holder);
        bill.setHolderNo(holder);
        bill.setProdNo(prod);
        bill.setRuleCode(rule);
        bill.setNodeNo(2);
        bill.setStatus(1);
        bill.setDelFlag(0);
        TGiprCertGrant g = new TGiprCertGrant();
        g.setId(seq.incrementAndGet());
        g.setGrantNo(grantNo);
        g.setBillId(bill.getId());
        g.setHolderNo(holder);
        g.setProdNo(prod);
        g.setRuleCode(rule);
        g.setFinishNode(2);
        g.setIssueDate(AT);
        g.setDelFlag(0);
        bill.setGrantId(g.getId());
        bills.put(bill.getId(), bill);
        grants.put(g.getId(), g);
        TGiprHolderBook h = new TGiprHolderBook();
        h.setHolderNo(holder);
        h.setProdNo(prod);
        h.setGrantId(g.getId());
        h.setStatus(1);
        h.setDelFlag(0);
        holders.put(holder, h);
        return g.getId();
    }

    // ================= 内存打桩 =================

    private static Object wp(AbstractWrapper<?, ?, ?> w, String column) {
        if (w == null || w.getSqlSegment() == null) {
            return null;
        }
        Matcher m = Pattern.compile(Pattern.quote(column) + "\\s*(=|>=|<=)\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}")
                .matcher(w.getSqlSegment());
        if (!m.find()) {
            return null;
        }
        return w.getParamNameValuePairs().get(m.group(2));
    }

    private void wireBill() {
        when(billMapper.insert(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                TGiprCertBill b = inv.getArgument(0);
                if (b.getId() == null) {
                    b.setId(seq.incrementAndGet());
                }
                bills.put(b.getId(), b);
                return 1;
            }
        });
        when(billMapper.selectById(any())).thenAnswer(new Answer<TGiprCertBill>() {
            @Override
            public TGiprCertBill answer(InvocationOnMock inv) {
                return bills.get(inv.getArgument(0));
            }
        });
        when(billMapper.updateById(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                return 1;
            }
        });
        when(billMapper.selectCount(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
                String seg = w.getSqlSegment();
                if (seg.contains("holder_no")) {
                    Object h = wp(w, "holder_no");
                    int n = 0;
                    for (TGiprCertBill b : bills.values()) {
                        if (h != null && h.equals(b.getHolderNo())
                                && (b.getStatus() == null || b.getStatus() == 0 || b.getStatus() == 2)
                                && (b.getDelFlag() == null || b.getDelFlag() == 0)) {
                            n++;
                        }
                    }
                    return n;
                }
                if (seg.contains("bill_no")) {
                    Object no = wp(w, "bill_no");
                    for (TGiprCertBill b : bills.values()) {
                        if (no != null && no.equals(b.getBillNo())) {
                            return 1;
                        }
                    }
                    return 0;
                }
                if (seg.contains("grant_id")) {
                    Object g = wp(w, "grant_id");
                    int n = 0;
                    for (TGiprCertBill b : bills.values()) {
                        if (g != null && g.equals(b.getGrantId())) {
                            n++;
                        }
                    }
                    return n;
                }
                return 0;
            }
        });
    }

    private void wireVote() {
        when(voteMapper.insert(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                TGiprCertVote v = inv.getArgument(0);
                if (v.getId() == null) {
                    v.setId(seq.incrementAndGet());
                }
                votes.put(v.getId(), v);
                return 1;
            }
        });
        when(voteMapper.deleteById(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                votes.remove(inv.getArgument(0));
                return 1;
            }
        });
        when(voteMapper.selectCount(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
                Object billId = wp(w, "bill_id");
                Object node = wp(w, "node_no");
                Object voter = wp(w, "voter_no");
                Object kind = wp(w, "vote_kind");
                int n = 0;
                for (TGiprCertVote v : votes.values()) {
                    if (v.getDelFlag() != null && v.getDelFlag() != 0) {
                        continue;
                    }
                    if (billId != null && !billId.equals(v.getBillId())) {
                        continue;
                    }
                    if (node != null && ((Number) node).intValue() != v.getNodeNo()) {
                        continue;
                    }
                    if (voter != null && !voter.equals(v.getVoterNo())) {
                        continue;
                    }
                    if (kind != null && ((Number) kind).intValue() != v.getVoteKind()) {
                        continue;
                    }
                    n++;
                }
                return n;
            }
        });
        when(voteMapper.selectList(any())).thenAnswer(new Answer<List<TGiprCertVote>>() {
            @Override
            public List<TGiprCertVote> answer(InvocationOnMock inv) {
                AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
                Object billId = wp(w, "bill_id");
                String seg = w.getSqlSegment() == null ? "" : w.getSqlSegment();
                // 关次条件只解析一次（Matcher 有状态，循环里反复 find 会失真）
                int nodeFrom = -1;
                int nodeEq = -1;
                Matcher geM = Pattern.compile("node_no\\s*>=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}").matcher(seg);
                Matcher eqM = Pattern.compile("node_no\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}").matcher(seg);
                if (geM.find()) {
                    nodeFrom = ((Number) w.getParamNameValuePairs().get(geM.group(1))).intValue();
                } else if (eqM.find()) {
                    nodeEq = ((Number) w.getParamNameValuePairs().get(eqM.group(1))).intValue();
                }
                List<TGiprCertVote> out = new ArrayList<TGiprCertVote>();
                for (TGiprCertVote v : votes.values()) {
                    if (billId != null && !billId.equals(v.getBillId())) {
                        continue;
                    }
                    if (v.getDelFlag() != null && v.getDelFlag() != 0) {
                        continue;
                    }
                    if (nodeFrom >= 0 && v.getNodeNo() < nodeFrom) {
                        continue;
                    }
                    if (nodeEq >= 0 && v.getNodeNo() != nodeEq) {
                        continue;
                    }
                    out.add(v);
                }
                return out;
            }
        });
    }

    private void wireSigner() {
        when(signerMapper.insert(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                TGiprCertSigner s = inv.getArgument(0);
                if (s.getId() == null) {
                    s.setId(seq.incrementAndGet());
                }
                signers.put(s.getId(), s);
                return 1;
            }
        });
        when(signerMapper.selectOne(any())).thenAnswer(new Answer<TGiprCertSigner>() {
            @Override
            public TGiprCertSigner answer(InvocationOnMock inv) {
                AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
                Object voter = wp(w, "voter_no");
                for (TGiprCertSigner s : signers.values()) {
                    if (voter != null && voter.equals(s.getVoterNo())
                            && (s.getDelFlag() == null || s.getDelFlag() == 0)) {
                        return s;
                    }
                }
                return null;
            }
        });
    }

    private void wireGrant() {
        when(grantMapper.insert(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                TGiprCertGrant g = inv.getArgument(0);
                if (g.getId() == null) {
                    g.setId(seq.incrementAndGet());
                }
                grants.put(g.getId(), g);
                return 1;
            }
        });
        when(grantMapper.selectById(any())).thenAnswer(new Answer<TGiprCertGrant>() {
            @Override
            public TGiprCertGrant answer(InvocationOnMock inv) {
                return grants.get(inv.getArgument(0));
            }
        });
        when(grantMapper.selectCount(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                // 核发当日已发数
                int n = 0;
                for (TGiprCertGrant g : grants.values()) {
                    if (g.getDelFlag() == null || g.getDelFlag() == 0) {
                        String d = new SimpleDateFormat("yyyy-MM-dd").format(g.getIssueDate());
                        if ("2026-10-07".equals(d)) {
                            n++;
                        }
                    }
                }
                return n;
            }
        });
    }

    private void wireHolder() {
        // 起单即假定底册有该企业一行可改口
        when(holderMapper.update(any(), any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                if (holderUpdateResult == 0) {
                    return 0;
                }
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TGiprHolderBook> uw =
                        inv.getArgument(1);
                String holder = (String) wp(uw, "holder_no");
                String setSql = uw.getSqlSet() == null ? "" : uw.getSqlSet();
                TGiprHolderBook h = holders.get(holder);
                if (h == null) {
                    h = new TGiprHolderBook();
                    h.setHolderNo(holder);
                    h.setDelFlag(0);
                    holders.put(holder, h);
                }
                // SET 子句形如 col=#{ew.paramNameValuePairs.MPGENxx},...：按列取出同回钉入的值
                Matcher sm = Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)\\}")
                        .matcher(setSql);
                while (sm.find()) {
                    String col = sm.group(1);
                    Object val = uw.getParamNameValuePairs().get(sm.group(2));
                    if ("status".equals(col)) {
                        h.setStatus(((Number) val).intValue());
                    } else if ("prod_no".equals(col)) {
                        h.setProdNo(val == null ? null : val.toString());
                    } else if ("grant_id".equals(col) && val != null) {
                        h.setGrantId(((Number) val).longValue());
                    }
                }
                return 1;
            }
        });
        when(holderMapper.selectOne(any())).thenAnswer(new Answer<TGiprHolderBook>() {
            @Override
            public TGiprHolderBook answer(InvocationOnMock inv) {
                AbstractWrapper<?, ?, ?> w = inv.getArgument(0);
                Object holder = wp(w, "holder_no");
                Object grantId = wp(w, "grant_id");
                TGiprHolderBook h = holder == null ? null : holders.get(holder);
                if (h == null || (h.getDelFlag() != null && h.getDelFlag() != 0)) {
                    return null;
                }
                if (grantId != null && !grantId.equals(h.getGrantId())) {
                    return null;
                }
                return h;
            }
        });
    }
}
