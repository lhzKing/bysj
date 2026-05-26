package com.example.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.trace.common.BizException;
import com.example.trace.config.TraceDemoDataProperties;
import com.example.trace.entity.BasePartSpec;
import com.example.trace.entity.SysRole;
import com.example.trace.entity.SysUser;
import com.example.trace.entity.TraceNode;
import com.example.trace.entity.TraceUserNodeBinding;
import com.example.trace.mapper.BasePartSpecMapper;
import com.example.trace.mapper.SysRoleMapper;
import com.example.trace.mapper.SysUserMapper;
import com.example.trace.mapper.TraceNodeMapper;
import com.example.trace.mapper.TraceUserNodeBindingMapper;
import com.example.trace.security.PasswordEncoder;
import com.example.trace.service.TraceMasterDataSeedService;
import com.example.trace.service.impl.support.DemoMasterDataBlueprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds master data from {@link DemoMasterDataBlueprint}.
 *
 * <p>Idempotency is per business key:</p>
 * <ul>
 *   <li>sys_user.username — skipped if exists (password is NOT overwritten)</li>
 *   <li>trace_node.node_code — skipped if exists</li>
 *   <li>base_part_spec.part_code — skipped if exists</li>
 *   <li>trace_user_node_binding (user_id, node_id) — skipped if exists</li>
 * </ul>
 *
 * <p>Rows are committed inside a single {@code @Transactional} method. Total
 * volume is small (≤ 8+18+15+16 = 57 rows) so chunking is unnecessary.</p>
 */
@Service
public class TraceMasterDataSeedServiceImpl implements TraceMasterDataSeedService {

    private static final Logger log = LoggerFactory.getLogger(TraceMasterDataSeedServiceImpl.class);

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final TraceNodeMapper traceNodeMapper;
    private final BasePartSpecMapper partSpecMapper;
    private final TraceUserNodeBindingMapper bindingMapper;
    private final PasswordEncoder passwordEncoder;
    private final TraceDemoDataProperties traceDemoDataProperties;

    public TraceMasterDataSeedServiceImpl(SysUserMapper sysUserMapper,
                                          SysRoleMapper sysRoleMapper,
                                          TraceNodeMapper traceNodeMapper,
                                          BasePartSpecMapper partSpecMapper,
                                          TraceUserNodeBindingMapper bindingMapper,
                                          PasswordEncoder passwordEncoder,
                                          TraceDemoDataProperties traceDemoDataProperties) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.traceNodeMapper = traceNodeMapper;
        this.partSpecMapper = partSpecMapper;
        this.bindingMapper = bindingMapper;
        this.passwordEncoder = passwordEncoder;
        this.traceDemoDataProperties = traceDemoDataProperties;
    }

    @Override
    @Transactional
    public Map<String, Object> seedMasterData(String operator, String operatorRole) {
        ensureEnabled(operator, operatorRole);
        log.info("Master-data seed started: operator={}, role={}",
                normalize(operator), normalize(operatorRole));

        Map<String, Integer> users = seedDemoUsers();
        Map<String, Integer> nodes = seedTraceNodes();
        Map<String, Integer> parts = seedParts();
        Map<String, Integer> bindings = seedBindings();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("demoUsers", users);
        result.put("traceNodes", nodes);
        result.put("partSpecs", parts);
        result.put("userNodeBindings", bindings);

        log.info("Master-data seed completed: operator={}, role={}, users={}, nodes={}, parts={}, bindings={}",
                normalize(operator), normalize(operatorRole), users, nodes, parts, bindings);
        return result;
    }

    private Map<String, Integer> seedDemoUsers() {
        int inserted = 0;
        int skipped = 0;
        for (DemoMasterDataBlueprint.DemoUser u : DemoMasterDataBlueprint.DEMO_USERS) {
            SysUser existing = sysUserMapper.selectOne(
                    new QueryWrapper<SysUser>().eq("username", u.username()));
            if (existing != null) {
                skipped++;
                continue;
            }
            SysRole role = sysRoleMapper.selectOne(
                    new QueryWrapper<SysRole>().eq("role_code", u.roleCode()));
            if (role == null) {
                throw BizException.serverError(
                        "demo seed 失败：sys_role 中缺少 role_code=" + u.roleCode() + "，请先执行 schema_consolidated.sql");
            }
            SysUser user = new SysUser();
            user.setUsername(u.username());
            user.setPassword(passwordEncoder.encode(u.plainPassword()));
            user.setRoleId(role.getId());
            user.setStatus(1);
            user.setTokenVersion(0);
            sysUserMapper.insert(user);
            inserted++;
        }
        return counts(inserted, skipped);
    }

    private Map<String, Integer> seedTraceNodes() {
        int inserted = 0;
        int skipped = 0;
        for (DemoMasterDataBlueprint.DemoNode n : DemoMasterDataBlueprint.DEMO_NODES) {
            TraceNode existing = traceNodeMapper.selectOne(
                    new QueryWrapper<TraceNode>().eq("node_code", n.nodeCode()));
            if (existing != null) {
                skipped++;
                continue;
            }
            TraceNode node = new TraceNode();
            node.setNodeCode(n.nodeCode());
            node.setNodeName(n.nodeName());
            node.setNodeType(n.nodeType());
            node.setProvince(n.province());
            node.setCity(n.city());
            node.setAddress(n.address());
            node.setEnabled(true);
            traceNodeMapper.insert(node);
            inserted++;
        }
        return counts(inserted, skipped);
    }

    private Map<String, Integer> seedParts() {
        int inserted = 0;
        int skipped = 0;
        for (DemoMasterDataBlueprint.DemoPart p : DemoMasterDataBlueprint.DEMO_PARTS) {
            BasePartSpec existing = partSpecMapper.selectOne(
                    new QueryWrapper<BasePartSpec>().eq("part_code", p.partCode()));
            if (existing != null) {
                skipped++;
                continue;
            }
            BasePartSpec part = new BasePartSpec();
            part.setPartCode(p.partCode());
            part.setPartName(p.partName());
            part.setPartType(p.partType());
            part.setModel(p.model());
            part.setManufacturer(p.manufacturer());
            part.setUnit("件");
            part.setRemark(p.remark());
            part.setEnabled(true);
            partSpecMapper.insert(part);
            inserted++;
        }
        return counts(inserted, skipped);
    }

    private Map<String, Integer> seedBindings() {
        int inserted = 0;
        int skipped = 0;
        for (DemoMasterDataBlueprint.DemoBinding b : DemoMasterDataBlueprint.DEMO_BINDINGS) {
            SysUser user = sysUserMapper.selectOne(
                    new QueryWrapper<SysUser>().eq("username", b.username()));
            TraceNode node = traceNodeMapper.selectOne(
                    new QueryWrapper<TraceNode>().eq("node_code", b.nodeCode()));
            if (user == null || node == null) {
                // Either the user or node row failed an earlier insert step; skip silently.
                skipped++;
                continue;
            }
            TraceUserNodeBinding existing = bindingMapper.selectOne(
                    new QueryWrapper<TraceUserNodeBinding>()
                            .eq("user_id", user.getId())
                            .eq("node_id", node.getId()));
            if (existing != null) {
                skipped++;
                continue;
            }
            TraceUserNodeBinding binding = new TraceUserNodeBinding();
            binding.setUserId(user.getId());
            binding.setNodeId(node.getId());
            binding.setDefaultNode(b.defaultNode());
            binding.setEnabled(true);
            bindingMapper.insert(binding);
            inserted++;
        }
        return counts(inserted, skipped);
    }

    private void ensureEnabled(String operator, String operatorRole) {
        if (traceDemoDataProperties.isEnabled()) {
            return;
        }
        log.warn("Master-data seed rejected because demo endpoints are disabled: operator={}, role={}",
                normalize(operator), normalize(operatorRole));
        throw BizException.forbidden("当前环境已禁用示例数据管理接口");
    }

    private static Map<String, Integer> counts(int inserted, int skipped) {
        Map<String, Integer> map = new HashMap<>(2);
        map.put("inserted", inserted);
        map.put("skipped", skipped);
        return map;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
