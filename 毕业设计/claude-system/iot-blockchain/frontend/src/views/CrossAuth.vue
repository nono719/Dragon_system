<template>
  <div class="ca-page">
    <!-- ═══════════ HERO 头部 ═══════════ -->
    <div class="hero">
      <div class="hero-text">
        <div class="tag">🔐 Cross-Domain Authentication</div>
        <h2>异构域之间的去中心化身份互认</h2>
        <p>基于区块链智能合约，实现 IoT 设备跨管理域的去中心化身份认证。合约自动校验设备身份、域归属、预言机实时数据，无需可信第三方即可完成安全互认。</p>
      </div>
      <div class="stat-box">
        <div class="st"><div class="n">{{ stats.total }}</div><div class="l">总认证</div></div>
        <div class="sep"></div>
        <div class="st"><div class="n ok">{{ stats.granted }}</div><div class="l">通过</div></div>
        <div class="sep"></div>
        <div class="st"><div class="n fail">{{ stats.total - stats.granted }}</div><div class="l">拒绝</div></div>
        <div class="sep"></div>
        <div class="st">
          <div class="n" style="color:#ffd700">{{ stats.total > 0 ? Math.round(stats.granted / stats.total * 100) : 0 }}%</div>
          <div class="l">通过率</div>
        </div>
      </div>
    </div>

    <!-- ═══════════ 跨域拓扑可视化 ═══════════ -->
    <el-card class="panel" shadow="never">
      <template #header>
        <div class="hdr-row">
          <span class="hdr">🌐 跨域拓扑视图 <small>可视化展示各管理域及设备分布，点击设备/域发起跨域认证</small></span>
          <el-tag :type="oracleStatus.running ? 'success' : 'danger'" effect="dark" size="small">
            预言机: {{ oracleStatus.running ? '运行中' : '已停止' }}
            · 新鲜 {{ oracleStatus.fresh_devices }}/{{ oracleStatus.active_devices }}
          </el-tag>
        </div>
      </template>

      <div class="topo-container">
        <!-- SVG 拓扑图 -->
        <svg class="topo-svg" :viewBox="`0 0 ${topoWidth} ${topoHeight}`" ref="topoSvg">
          <defs>
            <radialGradient id="domainGrad0"><stop offset="0%" stop-color="#e8f4fd"/><stop offset="100%" stop-color="#bae0ff"/></radialGradient>
            <radialGradient id="domainGrad1"><stop offset="0%" stop-color="#f0fbe8"/><stop offset="100%" stop-color="#c8f0a0"/></radialGradient>
            <radialGradient id="domainGrad2"><stop offset="0%" stop-color="#fff7e6"/><stop offset="100%" stop-color="#ffd591"/></radialGradient>
            <radialGradient id="domainGrad3"><stop offset="0%" stop-color="#f9f0ff"/><stop offset="100%" stop-color="#d3adf7"/></radialGradient>
            <radialGradient id="chainGrad"><stop offset="0%" stop-color="#e6f7ff"/><stop offset="100%" stop-color="#91d5ff"/></radialGradient>
            <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#409EFF" />
            </marker>
            <marker id="arrowheadGreen" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#67C23A" />
            </marker>
            <marker id="arrowheadRed" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
              <polygon points="0 0, 10 3.5, 0 7" fill="#F56C6C" />
            </marker>
            <filter id="glow">
              <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
              <feMerge><feMergeNode in="coloredBlur"/><feMergeNode in="SourceGraphic"/></feMerge>
            </filter>
          </defs>

          <!-- 中间区块链节点 -->
          <g class="chain-node" :transform="`translate(${topoWidth/2}, ${topoHeight/2})`">
            <circle r="52" :fill="authAnimating ? '#e6f7ff' : '#f0f5ff'" stroke="#409EFF" stroke-width="2.5"
              :class="{'anim-pulse': authAnimating}" filter="url(#glow)"/>
            <text text-anchor="middle" dy="-8" font-size="22">⛓️</text>
            <text text-anchor="middle" dy="12" font-size="10" fill="#1890ff" font-weight="600">CrossDomain</text>
            <text text-anchor="middle" dy="24" font-size="9" fill="#8c8c8c">Auth Contract</text>
          </g>

          <!-- 域节点 -->
          <g v-for="(dn, di) in topoNodes" :key="`dom-${dn.id}`"
            :transform="`translate(${dn.x}, ${dn.y})`"
            class="domain-node"
            :class="{
              'selected-src': form.src_did && srcDomain === dn.id,
              'selected-tgt': form.target_domain_id === dn.id,
              'auth-success': lastAuthResult && lastAuthResult.granted && (lastAuthResult.src_domain_id === dn.id || lastAuthResult.target_domain_id === dn.id),
              'auth-fail': lastAuthResult && !lastAuthResult.granted && lastAuthResult.src_domain_id === dn.id
            }"
            @click="onTopoNodeClick(dn)">
            <circle :r="dn.radius" :fill="`url(#domainGrad${di % 4})`" stroke="#bbb" stroke-width="1.5"
              :stroke-dasharray="form.target_domain_id === dn.id ? '6 3' : 'none'"
              :class="{'anim-highlight': form.src_did && srcDomain === dn.id}"/>
            <text text-anchor="middle" :dy="-dn.radius + 22" font-size="11" font-weight="700" :fill="domainColors[di % 4]">
              {{ dn.name }}
            </text>
            <text text-anchor="middle" :dy="-dn.radius + 35" font-size="9" fill="#8c8c8c">
              {{ dn.id }}
            </text>
            <!-- 域内设备 -->
            <g v-for="(dev, dvi) in dn.devices" :key="`dev-${dev.did}`"
              :transform="`translate(${dev.lx}, ${dev.ly})`"
              class="device-dot"
              :class="{
                'dev-active': dev.status === 'Active',
                'dev-selected': form.src_did === dev.did
              }"
              @click.stop="selectSrcDevice(dev)">
              <circle r="10" :fill="dev.status === 'Active' ? '#f6ffed' : '#fff1f0'"
                :stroke="dev.did === form.src_did ? '#1890ff' : (dev.status === 'Active' ? '#52c41a' : '#ff4d4f')"
                stroke-width="2"/>
              <text text-anchor="middle" dy="4" font-size="9">{{ dev.status === 'Active' ? '📱' : '⚠️' }}</text>
              <title>{{ dev.device_id }} ({{ dev.status }})</title>
            </g>
            <!-- 设备计数 -->
            <text text-anchor="middle" :dy="dn.radius - 12" font-size="9" fill="#8c8c8c">
              {{ dn.devices.length }} 设备 · {{ dn.devices.filter(d => d.status === 'Active').length }} 活跃
            </text>
          </g>

          <!-- 认证连线动画 -->
          <g v-if="authAnimStep >= 1 && form.src_did && form.target_domain_id">
            <!-- 源域 → 合约 -->
            <line :x1="srcTopoNode?.x" :y1="srcTopoNode?.y"
              :x2="topoWidth/2" :y2="topoHeight/2"
              :stroke="authAnimStep >= 2 ? '#409EFF' : '#d9d9d9'" stroke-width="2.5"
              stroke-dasharray="8 4" marker-end="url(#arrowhead)"
              :class="{'anim-dash': authAnimStep === 1}"/>
            <!-- 合约 → 目标域 -->
            <line :x1="topoWidth/2" :y1="topoHeight/2"
              :x2="tgtTopoNode?.x" :y2="tgtTopoNode?.y"
              :stroke="authAnimStep >= 4 ? (lastAuthResult?.granted ? '#67C23A' : '#F56C6C') : '#d9d9d9'"
              stroke-width="2.5" stroke-dasharray="8 4"
              :marker-end="authAnimStep >= 4 ? (lastAuthResult?.granted ? 'url(#arrowheadGreen)' : 'url(#arrowheadRed)') : 'url(#arrowhead)'"
              :class="{'anim-dash': authAnimStep === 3}"/>
          </g>

          <!-- 预言机连线 -->
          <line v-if="srcTopoNode && authAnimStep >= 1"
            :x1="topoWidth/2 - 40" :y1="topoHeight/2 + 50"
            :x2="topoWidth/2 + 40" :y2="topoHeight/2 + 50"
            stroke="#E6A23C" stroke-width="1" stroke-dasharray="4 2" opacity="0.5"/>
          <text v-if="authAnimStep >= 2" :x="topoWidth/2" :y="topoHeight/2 + 65"
            text-anchor="middle" font-size="9" fill="#E6A23C">🛰️ 预言机验证</text>
        </svg>

        <!-- 拓扑图右侧信息面板 -->
        <div class="topo-info">
          <div class="topo-legend">
            <div class="legend-title">图例说明</div>
            <div class="legend-item"><span class="dot" style="background:#52c41a"></span> Active 设备</div>
            <div class="legend-item"><span class="dot" style="background:#ff4d4f"></span> 未激活/已撤销</div>
            <div class="legend-item"><span class="dot" style="background:#1890ff"></span> 当前选中的源设备</div>
            <div class="legend-item"><span class="line-sample solid"></span> 认证成功路径</div>
            <div class="legend-item"><span class="line-sample dashed"></span> 认证进行中</div>
          </div>
          <div class="topo-tip" v-if="!form.src_did">
            💡 点击域内 Active 设备作为<b>源设备</b>，再点击其他域作为<b>目标域</b>
          </div>
          <div class="topo-tip active" v-else-if="!form.target_domain_id">
            ✅ 已选源设备: <code>{{ src?.device_id }}</code><br/>
            👉 请点击其他域作为<b>目标域</b>
          </div>
          <div class="topo-tip ready" v-else>
            🚀 源: <code>{{ src?.device_id }}</code> → 目标: <code>{{ form.target_domain_id }}</code><br/>
            点击下方按钮发起认证
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════════ 跨域认证 VS 传统认证对比 ═══════════ -->
    <el-card class="panel" shadow="never">
      <template #header><span class="hdr">⚖️ 安全对比: 区块链跨域认证 vs 传统中心化认证</span></template>
      <div class="compare-grid">
        <div class="compare-col traditional">
          <div class="compare-head">
            <span class="compare-icon">🏢</span>
            <span class="compare-title">传统中心化认证</span>
          </div>
          <div class="compare-body">
            <div class="compare-item bad">
              <span class="ci-icon">❌</span>
              <div class="ci-text">
                <div class="ci-label">单点故障风险</div>
                <div class="ci-desc">中心认证服务器宕机则全域瘫痪</div>
              </div>
            </div>
            <div class="compare-item bad">
              <span class="ci-icon">❌</span>
              <div class="ci-text">
                <div class="ci-label">信任前提</div>
                <div class="ci-desc">所有域必须信任同一中心 CA</div>
              </div>
            </div>
            <div class="compare-item bad">
              <span class="ci-icon">❌</span>
              <div class="ci-text">
                <div class="ci-label">不可审计</div>
                <div class="ci-desc">认证记录可被篡改，审计困难</div>
              </div>
            </div>
            <div class="compare-item bad">
              <span class="ci-icon">❌</span>
              <div class="ci-text">
                <div class="ci-label">跨组织协作困难</div>
                <div class="ci-desc">异构域需额外集成，成本高</div>
              </div>
            </div>
            <div class="compare-item warn">
              <span class="ci-icon">⚠️</span>
              <div class="ci-text">
                <div class="ci-label">无实时设备状态校验</div>
                <div class="ci-desc">仅验证凭证有效性，不关心设备当前状态</div>
              </div>
            </div>
          </div>
          <div class="compare-flow">
            <div class="cf-step">设备 A</div>
            <div class="cf-arrow">→</div>
            <div class="cf-step highlight-bad">中心认证服务器</div>
            <div class="cf-arrow">→</div>
            <div class="cf-step">设备 B</div>
          </div>
        </div>

        <div class="compare-vs">VS</div>

        <div class="compare-col blockchain">
          <div class="compare-head">
            <span class="compare-icon">⛓️</span>
            <span class="compare-title">区块链跨域认证 (本系统)</span>
          </div>
          <div class="compare-body">
            <div class="compare-item good">
              <span class="ci-icon">✅</span>
              <div class="ci-text">
                <div class="ci-label">去中心化 · 无单点故障</div>
                <div class="ci-desc">合约部署在链上，分布式节点共同维护</div>
              </div>
            </div>
            <div class="compare-item good">
              <span class="ci-icon">✅</span>
              <div class="ci-text">
                <div class="ci-label">零信任 · 代码即法律</div>
                <div class="ci-desc">智能合约自动判决，无需可信第三方</div>
              </div>
            </div>
            <div class="compare-item good">
              <span class="ci-icon">✅</span>
              <div class="ci-text">
                <div class="ci-label">不可篡改 · 完全可审计</div>
                <div class="ci-desc">每次认证上链，TX Hash 永久可追溯</div>
              </div>
            </div>
            <div class="compare-item good">
              <span class="ci-icon">✅</span>
              <div class="ci-text">
                <div class="ci-label">异构域原生互通</div>
                <div class="ci-desc">任意域注册即可跨域互认，零额外集成</div>
              </div>
            </div>
            <div class="compare-item good">
              <span class="ci-icon">✅</span>
              <div class="ci-text">
                <div class="ci-label">预言机实时状态校验</div>
                <div class="ci-desc">合约检查设备在线状态、信号强度、数据新鲜度</div>
              </div>
            </div>
          </div>
          <div class="compare-flow">
            <div class="cf-step">源域设备</div>
            <div class="cf-arrow green">→</div>
            <div class="cf-step highlight-good">智能合约 (链上)</div>
            <div class="cf-arrow green">→</div>
            <div class="cf-step">目标域</div>
            <div class="cf-oracle">🛰️ 预言机实时喂送</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════════ 演示场景一键预设 ═══════════ -->
    <el-card class="panel" shadow="never" v-if="scenarios.length">
      <template #header><span class="hdr">🎯 跨域演示场景 <small>一键预填充典型的跨域协作场景</small></span></template>
      <div class="scenarios">
        <div v-for="s in scenarios" :key="s.key" class="scenario-card" :class="{active: selectedScenario===s.key}" @click="applyScenario(s)">
          <div class="sc-hd">
            <div class="sc-title">{{ s.title }}</div>
            <el-tag v-for="sc in s.requested_scopes" :key="`sc-${s.key}-${sc}`" size="small" :type="scopeTagType(sc)">{{ scopeLabel(sc) }}</el-tag>
          </div>
          <div class="sc-story">{{ s.story }}</div>
          <div class="sc-flow">
            <el-tag size="small">{{ s.src_domain_id }}</el-tag>
            <el-icon><Right/></el-icon>
            <el-tag size="small" type="warning">{{ s.target_domain_id }}</el-tag>
            <span class="muted" v-if="s.suggested_src">建议源设备: {{ s.suggested_src.device_id }}</span>
            <span class="muted" v-else style="color:#F56C6C">当前源域暂无 Active 设备</span>
          </div>
          <div class="sc-hl">
            <el-tag v-for="h in s.highlight" :key="`hl-${s.key}-${h}`" size="small" effect="plain">{{ h }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════════ 认证流程 (增强版) ═══════════ -->
    <el-card class="panel" shadow="never">
      <template #header><span class="hdr">🎬 认证流程 · 四步校验</span></template>

      <div class="oracle-status-bar">
        <el-tag :type="oracleStatus.running ? 'success' : 'danger'" effect="dark">
          预言机: {{ oracleStatus.running ? '运行中' : '已停止' }}
        </el-tag>
        <span class="muted">
          上次喂送: {{ oracleStatus.last_tick_at ? formatAgo(oracleStatus.last_tick_at) : '无' }}
          · 新鲜设备 {{ oracleStatus.fresh_devices }}/{{ oracleStatus.active_devices }}
        </span>
        <el-tag v-if="srcRuntimeHealth === 'fresh'" type="success">源设备数据新鲜</el-tag>
        <el-tag v-else-if="srcRuntimeHealth === 'stale'" type="warning">源设备数据过期</el-tag>
        <el-tag v-else-if="srcRuntimeHealth === 'missing'" type="danger">源设备无预言机数据</el-tag>
      </div>

      <!-- 增强的流程可视化 -->
      <div class="flow-enhanced">
        <div class="flow-node src" :class="{active: step>=1}">
          <div class="fn-icon">🏠</div>
          <div class="fn-title">源管理域</div>
          <div class="fn-sub">{{ srcDomain || '—' }}</div>
          <div v-if="src" class="fn-device">
            <span class="fn-dev-id">📱 {{ src.device_id }}</span>
            <code class="fn-did">{{ src.did?.slice(0, 14) }}…</code>
          </div>
          <div v-if="srcRT" class="fn-runtime">
            <el-tag :type="srcRT.online ? 'success' : 'danger'" size="small">{{ srcRT.online ? '在线' : '离线' }}</el-tag>
            <span class="fn-signal">📶 {{ srcRT.signal_dbm }} dBm</span>
          </div>
        </div>

        <div class="flow-connector" :class="{active: step>=1}">
          <div class="fc-line"></div>
          <div class="fc-label">发起请求</div>
          <div class="fc-particles" v-if="step === 1">
            <span class="particle p1"></span>
            <span class="particle p2"></span>
            <span class="particle p3"></span>
          </div>
        </div>

        <div class="flow-node contract" :class="{active: step>=2, fail: step>=4 && result && !result.granted}">
          <div class="fn-icon">⛓️</div>
          <div class="fn-title">CrossDomainAuth</div>
          <div class="fn-sub">智能合约校验</div>
          <div class="check-list">
            <div class="check-item" :class="checkState(1)">
              <span class="ck-dot"></span>
              <span class="ck-num">①</span>
              <span class="ck-text">设备身份有效</span>
              <span class="ck-detail">DeviceRegistry 查询</span>
            </div>
            <div class="check-item" :class="checkState(2)">
              <span class="ck-dot"></span>
              <span class="ck-num">②</span>
              <span class="ck-text">跨域请求校验</span>
              <span class="ck-detail">源域 ≠ 目标域</span>
            </div>
            <div class="check-item" :class="checkState(3)">
              <span class="ck-dot"></span>
              <span class="ck-num">③</span>
              <span class="ck-text">预言机数据新鲜</span>
              <span class="ck-detail">OracleFeed ≤ 300s</span>
            </div>
            <div class="check-item" :class="checkState(4)">
              <span class="ck-dot"></span>
              <span class="ck-num">④</span>
              <span class="ck-text">运行状态合规</span>
              <span class="ck-detail">在线 & 信号 ≥ -90dBm</span>
            </div>
          </div>
        </div>

        <div class="flow-connector" :class="{active: step>=3}">
          <div class="fc-line" :class="{green: result?.granted, red: result && !result.granted}"></div>
          <div class="fc-label">{{ result?.granted ? '✅ 授权' : (step>=4 ? '❌ 拒绝' : '…') }}</div>
          <div class="fc-particles" v-if="step === 3 && result?.granted">
            <span class="particle p1 green"></span>
            <span class="particle p2 green"></span>
            <span class="particle p3 green"></span>
          </div>
        </div>

        <div class="flow-node target" :class="{active: step>=3 && result?.granted, fail: step>=4 && result && !result.granted}">
          <div class="fn-icon">🏭</div>
          <div class="fn-title">目标管理域</div>
          <div class="fn-sub">{{ form.target_domain_id || '—' }}</div>
          <div v-if="result?.granted" class="fn-access-badge">
            <el-tag type="success" effect="dark" size="small">可访问资源</el-tag>
          </div>
        </div>
      </div>

      <!-- 预言机数据条 -->
      <div class="oracle-feed">
        <div class="o-icon">🛰️</div>
        <div class="o-text">
          <b>预言机实时喂送 → </b>
          <span v-if="srcRT">
            在线 <el-tag :type="srcRT.online?'success':'info'" size="small">{{ srcRT.online?'YES':'NO' }}</el-tag>
            · 信号 <b>{{ srcRT.signal_dbm }}</b> dBm
            · 位置 <code>{{ srcRT.geo_hash }}</code>
          </span>
          <span v-else class="muted">请选择源设备</span>
        </div>
      </div>

      <!-- 操作区 -->
      <div class="form-line">
        <el-select v-model="form.src_did" filterable placeholder="① 选择源域 Active 设备" style="min-width:340px">
          <el-option-group v-for="group in groupedDevices" :key="group.domain" :label="group.domain">
            <el-option v-for="d in group.list" :key="d.did"
              :label="`${d.device_id} (${d.did.slice(0,10)}…)`" :value="d.did"/>
          </el-option-group>
        </el-select>
        <el-icon class="arr-icon"><Right/></el-icon>
        <el-select v-model="form.target_domain_id" placeholder="② 选择目标域" style="min-width:240px">
          <el-option v-for="d in targetDomains" :key="d.domain_id" :label="d.name" :value="d.domain_id"/>
        </el-select>
        <el-select v-model="form.requested_scopes" multiple collapse-tags collapse-tags-tooltip
          placeholder="③ 申请权限(只读/控制/存储)" style="min-width:280px">
          <el-option label="只读 (传感/查询)" value="readonly" />
          <el-option label="控制 (执行命令)" value="control" />
          <el-option label="存储 (读写存储)" value="storage" />
        </el-select>
        <el-button size="large" :loading="preflightLoading" @click="runPreflight"
          :disabled="!form.src_did || !form.target_domain_id">🔎 风险预评估</el-button>
        <el-button type="primary" size="large" :loading="loading" @click="submit"
          :disabled="!form.src_did || !form.target_domain_id">🚀 发起跨域认证</el-button>
      </div>

      <!-- 风险预评估结果 -->
      <div v-if="preflight" class="preflight-panel" :class="preflight.predict_granted ? 'pf-ok' : 'pf-fail'">
        <div class="pf-hd">
          <el-tag :type="preflight.predict_granted ? 'success' : 'danger'" effect="dark">
            {{ preflight.predict_granted ? '✅ 预计可通过' : '⚠️ 预计将被拒绝' }}
          </el-tag>
          <span class="muted">{{ preflight.predict_reason }}</span>
          <el-tag :type="riskTagType(preflight.risk?.level)" size="small">
            风险: {{ preflight.risk?.level }} ({{ preflight.risk?.score }})
          </el-tag>
          <span class="muted" v-if="preflight.runtime">
            ⏱ {{ preflight.runtime.age_sec }}s 前 · 信号 {{ preflight.runtime.signal_dbm }} dBm · 在线 {{ preflight.runtime.online ? 'YES':'NO' }}
          </span>
        </div>
        <div class="pf-checks">
          <div v-for="c in preflight.checks" :key="c.key" class="pf-chk" :class="'s-'+c.status">
            <span class="pf-dot"></span>
            <div class="pf-body">
              <div class="pf-label">{{ c.label }} <em class="pf-status">{{ pfStatusText(c.status) }}</em></div>
              <div class="pf-detail">{{ c.detail }}</div>
            </div>
          </div>
        </div>
        <div v-if="preflight.hints?.length" class="pf-hints">
          <b>💡 建议:</b>
          <ul><li v-for="(h,i) in preflight.hints" :key="`hint-${i}`">{{ h }}</li></ul>
        </div>
      </div>
    </el-card>

    <!-- ═══════════ 认证结果 ═══════════ -->
    <el-card v-if="result" class="panel result-card" :class="result.granted?'r-ok':'r-fail'" shadow="never">
      <template #header>
        <div class="result-hd">
          <el-tag :type="result.granted?'success':'danger'" size="large" effect="dark">
            {{ result.granted? '✅ 跨域认证通过' : '❌ 跨域认证拒绝' }}
          </el-tag>
          <span class="reason">{{ result.reason }}</span>
          <span v-if="result.granted && session && !session.revoked" class="countdown">
            会话有效: <b>{{ session.remaining_sec }}</b> 秒
          </span>
          <el-tag v-if="session?.revoked" type="danger" effect="dark" size="large">
            会话已撤销: {{ session.revoke_reason || '—' }}
          </el-tag>
          <el-button v-if="result.granted && session && !session.revoked && session.remaining_sec>0"
            size="small" type="danger" plain @click="revokeCurrentSession">撤销会话</el-button>
        </div>
      </template>

      <!-- 链上凭证信息 -->
      <div class="chain-proof">
        <div class="proof-title">🔗 链上认证凭证 (不可篡改)</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="Session ID" :span="2">
            <code class="mono-wrap">{{ result.session_id }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="源设备 DID" :span="2">
            <code class="mono-wrap">{{ result.src_did }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="源域">{{ result.src_domain_id }}</el-descriptions-item>
          <el-descriptions-item label="目标域">{{ result.target_domain_id }}</el-descriptions-item>
          <el-descriptions-item label="区块号">#{{ result.block_number }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ result.created_at }}</el-descriptions-item>
          <el-descriptions-item label="交易哈希" :span="2">
            <code class="mono-wrap">{{ result.tx_hash }}</code>
            <el-tag size="small" type="info" style="margin-left:8px">永久可追溯</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请权限" :span="2">
            <el-tag v-for="s in form.requested_scopes" :key="`req-${s}`" size="small" style="margin-right:6px">
              {{ scopeLabel(s) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 对比: 如果没有跨域认证会怎样 -->
      <div v-if="result.granted" class="what-if-panel">
        <div class="wif-title">💡 跨域认证的价值 — 如果没有区块链跨域认证？</div>
        <div class="wif-grid">
          <div class="wif-item">
            <div class="wif-icon bad">🚫</div>
            <div class="wif-label">无法验证设备真实身份</div>
            <div class="wif-desc">传统方案中，目标域无法独立验证源设备是否合法注册，存在伪造设备攻击风险</div>
          </div>
          <div class="wif-item">
            <div class="wif-icon bad">🚫</div>
            <div class="wif-label">认证记录可被篡改</div>
            <div class="wif-desc">中心化日志可被管理员修改，而链上 TX {{ result.tx_hash?.slice(0,10) }}… 不可篡改</div>
          </div>
          <div class="wif-item">
            <div class="wif-icon bad">🚫</div>
            <div class="wif-label">无实时设备状态校验</div>
            <div class="wif-desc">预言机提供了设备在线状态和信号强度校验，传统方案缺少此维度</div>
          </div>
          <div class="wif-item">
            <div class="wif-icon good">✅</div>
            <div class="wif-label">本次认证全程链上</div>
            <div class="wif-desc">区块 #{{ result.block_number }}，合约自动执行 4 项检查，零人工干预</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- ═══════════ 资源访问面板 ═══════════ -->
    <el-card v-if="result && result.granted" class="panel resources-card" shadow="never">
      <template #header>
        <div class="hdr-row">
          <span class="hdr">🎯 访问目标域资源 <small>(凭 Session 在 {{ session?.remaining_sec || 0 }}s 内有效)</small></span>
          <div style="display:flex; gap:8px;">
            <el-button size="small" @click="refreshLiveData" :loading="liveRefreshing">刷新实时数据</el-button>
            <el-button size="small" @click="loadRecords">刷新记录</el-button>
          </div>
        </div>
      </template>
      <div v-if="permissions" class="perm-panel">
        <div class="perm-hd">
          <el-tag :type="permissions.valid ? 'success' : 'danger'" effect="dark">
            {{ permissions.valid ? '权限有效' : '权限无效' }}
          </el-tag>
          <span class="muted">
            资源授权 {{ Object.keys(permissions.resource_actions || {}).length }} 项 · Scope {{ permissions.scope_list?.length || 0 }} 项
          </span>
          <span class="muted">
            申请: {{ (permissions.requested_scopes || []).map(scopeLabel).join(' / ') || '无' }}
            · 实际授予: {{ (permissions.granted_scopes || []).map(scopeLabel).join(' / ') || '无' }}
          </span>
        </div>
        <div class="perm-hd" v-if="approval">
          <el-tag :type="approvalTagType(approval.status)">
            审批状态: {{ approvalText(approval.status) }}
          </el-tag>
          <span class="muted" v-if="approval.pending_scopes?.length">
            待审批: {{ approval.pending_scopes.map(scopeLabel).join(' / ') }}
          </span>
          <span class="muted" v-if="approval.note">备注: {{ approval.note }}</span>
          <span class="muted" v-if="approval.approver">审批人: {{ approval.approver }}</span>
          <span class="muted" v-if="approval.updated_at">审批时间: {{ formatDateTime(approval.updated_at) }}</span>
          <el-button v-if="approval.status==='pending'" size="small" type="success" @click="approveCurrentSession">批准高权限</el-button>
          <el-button v-if="approval.status==='pending'" size="small" type="danger" @click="rejectCurrentSession">驳回高权限</el-button>
        </div>
        <div class="perm-scopes">
          <el-tag v-for="s in (permissions.scope_list || [])" :key="s" size="small" type="info">{{ s }}</el-tag>
          <span v-if="!(permissions.scope_list || []).length" class="muted">当前会话未授予任何可用 scope</span>
        </div>
        <div class="approval-history" v-if="approvalHistory.length">
          <div class="muted" style="margin:8px 0 6px;">审批流水</div>
          <div class="approval-event" v-for="(ev, idx) in approvalHistory" :key="`apv-${idx}-${ev.at}`">
            <el-tag size="small" :type="eventTagType(ev.action)">{{ eventText(ev.action) }}</el-tag>
            <span>{{ formatDateTime(ev.at) }}</span>
            <span>操作人: {{ ev.operator || '-' }}</span>
            <span v-if="ev.scopes?.length">范围: {{ ev.scopes.map(scopeLabel).join(' / ') }}</span>
            <span v-if="ev.note">备注: {{ ev.note }}</span>
          </div>
        </div>
      </div>

      <div v-for="m in moduleGroups" :key="`mod-${m.key}`" class="module-block">
        <div class="module-hd">
          <span class="module-title">{{ m.title }}</span>
          <span class="muted">{{ m.desc }}</span>
        </div>
        <el-row :gutter="14" v-if="m.list.length">
          <el-col :span="8" v-for="r in m.list" :key="r.id" style="margin-bottom:14px">
            <div class="res-card" :class="'t-'+r.resource_type">
              <div class="res-top">
                <div class="res-icon">{{ resIcon(r.resource_type) }}</div>
                <el-tag size="small" :type="resTagType(r.resource_type)">{{ resTypeLabel(r.resource_type) }}</el-tag>
              </div>
              <div class="res-name">{{ showText(r.name || (`资源 #${r.id}`)) }}</div>
              <div class="res-name">{{ displayResName(r) }}</div>
              <div class="res-desc">{{ displayResDesc(r) }}</div>
              <div class="res-extra" v-if="r.resource_type === 'sensor_data'">
                <el-tag size="small" :type="r._metaOnline ? 'success' : 'info'">在线: {{ r._metaOnline ? 'YES' : 'NO' }}</el-tag>
                <el-tag size="small">信号: {{ r._metaSignal ?? '--' }} dBm</el-tag>
                <el-tag size="small">位置: {{ r._metaGeo || '--' }}</el-tag>
              </div>
              <div class="res-extra" v-if="r.resource_type === 'query'">
                <el-tag size="small">活跃设备: {{ r._metaActive ?? '--' }}</el-tag>
                <el-tag size="small">在线设备: {{ r._metaOnlineCount ?? '--' }}</el-tag>
                <el-tag size="small">平均信号: {{ r._metaAvgSignal ?? '--' }}</el-tag>
              </div>
              <div class="res-extra" v-if="r.resource_type === 'control'">
                <el-select v-model="r._ctrlCommand" size="small" style="width:100%; margin-bottom:6px">
                  <el-option label="重启设备" value="reboot" />
                  <el-option label="刷新配置" value="reload_config" />
                  <el-option label="切换节能模式" value="eco_mode" />
                </el-select>
                <el-input-number v-model="r._ctrlPriority" :min="1" :max="5" size="small" style="width:100%" />
              </div>
              <div class="res-extra" v-if="r.resource_type === 'storage'">
                <el-input v-model="r._storeKey" size="small" placeholder="存储键，例如 profile.tempThreshold" style="margin-bottom:6px" />
                <el-input v-model="r._storeValue" size="small" placeholder="写入值（WRITE时生效）" />
              </div>
              <div class="res-live" v-if="r.resource_type !== 'control'">
                <div class="res-live-hd">
                  <span>实时预览</span>
                  <span class="res-live-time">{{ r._liveAt ? formatTime(r._liveAt) : '--' }}</span>
                </div>
                <pre class="res-live-body">{{ r._liveText || '等待拉取实时数据…' }}</pre>
              </div>
              <div class="perm-actions">
                <el-tag v-for="a in (permissions?.resource_actions?.[r.id] || [])" :key="`${r.id}-${a}`" size="small">
                  {{ a.toUpperCase() }}
                </el-tag>
                <el-tag v-if="!(permissions?.resource_actions?.[r.id] || []).length" type="danger" size="small">无权限</el-tag>
              </div>
              <el-select v-if="(permissions?.resource_actions?.[r.id] || []).length > 1" v-model="r._selectedAction"
                size="small" style="width:100%; margin-bottom:8px">
                <el-option v-for="a in permissions.resource_actions[r.id]" :key="`${r.id}-pick-${a}`" :label="a.toUpperCase()" :value="a"/>
              </el-select>
              <el-button type="primary" size="small" :loading="r._loading" @click="access(r)" style="width:100%"
                :disabled="!resourceAction(r)">
                <el-icon><Promotion/></el-icon>&nbsp;{{ actionLabel(resourceAction(r)) }}
              </el-button>
            </div>
          </el-col>
        </el-row>
        <el-empty v-else :description="`${m.title}暂无动态资源`" :image-size="64"/>
      </div>

      <!-- 调用结果 & 历史 -->
      <el-divider content-position="left">调用历史 (本会话)</el-divider>
      <div class="rec-list">
        <div v-for="rec in records" :key="rec.id" class="rec" :class="rec.status">
          <div class="rec-hd">
            <el-tag size="small" :type="recTagType(rec.status)" effect="dark">{{ recLabel(rec.status) }}</el-tag>
            <span class="rec-name">{{ showText(rec.resource_name || ('资源 #'+rec.resource_id)) }}</span>
            <span class="rec-time">{{ formatTime(rec.created_at) }} · {{ rec.latency_ms }}ms</span>
          </div>
          <pre class="rec-body">{{ prettyJson(rec.response) }}</pre>
        </div>
        <div v-if="!records.length" class="muted" style="text-align:center; padding:20px">点击上方资源卡片发起调用</div>
      </div>
      <el-divider content-position="left">控制命令执行结果</el-divider>
      <div class="spec-list">
        <div v-for="c in controlExecList" :key="`ctrl-${c.id}`" class="spec-item">
          <el-tag size="small" type="warning">EXEC</el-tag>
          <span>{{ formatDateTime(c.created_at) }}</span>
          <span>{{ c.resource_name || (`资源#${c.resource_id}`) }}</span>
          <span>命令: {{ c.command || '-' }}</span>
          <span>优先级: {{ c.priority ?? '-' }}</span>
          <span>结果: {{ c.ok ? '成功' : '失败' }}</span>
        </div>
        <div v-if="!controlExecList.length" class="muted" style="text-align:center; padding:12px">暂无控制命令记录</div>
      </div>
      <el-divider content-position="left">存储写入历史</el-divider>
      <div class="spec-list">
        <div v-for="s in storageWriteList" :key="`store-${s.id}`" class="spec-item">
          <el-tag size="small" type="success">WRITE</el-tag>
          <span>{{ formatDateTime(s.created_at) }}</span>
          <span>{{ s.resource_name || (`资源#${s.resource_id}`) }}</span>
          <span>Key: {{ s.key || '-' }}</span>
          <span>Value: {{ s.value || '-' }}</span>
          <span>结果: {{ s.ok ? '成功' : '失败' }}</span>
        </div>
        <div v-if="!storageWriteList.length" class="muted" style="text-align:center; padding:12px">暂无存储写入记录</div>
      </div>

      <el-divider content-position="left">🕒 跨域活动时间线</el-divider>
      <div class="timeline">
        <div v-for="(ev, idx) in timeline" :key="`tl-${idx}`" class="tl-item" :class="'k-'+ev.kind+' s-'+ev.status">
          <div class="tl-marker">{{ timelineIcon(ev.kind) }}</div>
          <div class="tl-body">
            <div class="tl-hd">
              <span class="tl-title">{{ ev.title }}</span>
              <el-tag size="small" :type="timelineStatusTag(ev.status)">{{ timelineStatusText(ev.status) }}</el-tag>
              <span class="tl-time">{{ formatDateTime(ev.at) }}</span>
            </div>
            <div class="tl-detail" v-if="ev.actor || ev.detail">
              <span v-if="ev.actor" class="muted">操作方: {{ ev.actor }}</span>
              <span v-if="ev.detail" class="tl-note">{{ shortDetail(ev.detail) }}</span>
            </div>
            <div class="tl-extra" v-if="ev.kind==='auth' && ev.extra">
              <el-tag size="small" :type="riskTagType(ev.extra.risk_level)">风险 {{ ev.extra.risk_level }} ({{ ev.extra.risk_score }})</el-tag>
              <span class="muted">区块 #{{ ev.extra.block_number }}</span>
            </div>
            <div class="tl-extra" v-if="ev.kind==='access' && ev.extra">
              <el-tag size="small">{{ (ev.extra.action || '').toUpperCase() }}</el-tag>
              <span class="muted">耗时 {{ ev.extra.latency_ms }}ms</span>
            </div>
          </div>
        </div>
        <div v-if="!timeline.length" class="muted" style="text-align:center; padding:12px">暂无时间线事件</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Right, Promotion } from '@element-plus/icons-vue'
import api from '../api'

/* ───── 状态 ───── */
const form = reactive({ src_did: '', target_domain_id: '', requested_scopes: ['readonly', 'control'] })
const devices = ref([]), domains = ref([])
const result = ref(null), loading = ref(false), step = ref(0)
const srcRT = ref(null)
const oracleStatus = reactive({ running: false, last_tick_at: '', active_devices: 0, fresh_devices: 0 })
const session = ref(null)
const permissions = ref(null)
const approval = ref(null)
const approvalHistory = ref([])
const resources = ref([]), records = ref([])
const stats = reactive({ total: 0, granted: 0 })
let sessionTimer = null, liveTimer = null, oracleStatusTimer = null
const liveRefreshing = ref(false)
const scenarios = ref([])
const selectedScenario = ref('')
const preflight = ref(null)
const preflightLoading = ref(false)
const timeline = ref([])

// 拓扑动画
const authAnimating = ref(false)
const authAnimStep = ref(0)
const lastAuthResult = ref(null)
const topoSvg = ref(null)
const topoWidth = 800
const topoHeight = 480
const domainColors = ['#1890ff', '#52c41a', '#fa8c16', '#722ed1']

/* ───── 拓扑计算 ───── */
const topoNodes = computed(() => {
  const doms = domains.value
  if (!doms.length) return []
  const cx = topoWidth / 2, cy = topoHeight / 2
  const radiusOrbit = Math.min(topoWidth, topoHeight) * 0.36
  return doms.map((d, i) => {
    const angle = (2 * Math.PI * i / doms.length) - Math.PI / 2
    const x = cx + radiusOrbit * Math.cos(angle)
    const y = cy + radiusOrbit * Math.sin(angle)
    const devs = devices.value.filter(dev => dev.domain_id === d.domain_id)
    const radius = Math.max(55, 40 + devs.length * 6)
    // 域内设备布局
    const devsWithLayout = devs.map((dev, di) => {
      const da = (2 * Math.PI * di / Math.max(devs.length, 1))
      const dr = radius * 0.55
      return { ...dev, lx: dr * Math.cos(da), ly: dr * Math.sin(da) + 6 }
    })
    return { ...d, id: d.domain_id, x, y, radius, devices: devsWithLayout }
  })
})

const srcTopoNode = computed(() => topoNodes.value.find(n => n.id === srcDomain.value))
const tgtTopoNode = computed(() => topoNodes.value.find(n => n.id === form.target_domain_id))

function onTopoNodeClick(node) {
  if (!form.src_did) {
    // 没选源设备: 选该域的第一个 Active 设备
    const activeDev = node.devices.find(d => d.status === 'Active')
    if (activeDev) {
      form.src_did = activeDev.did
      ElMessage.success(`已选源设备: ${activeDev.device_id}`)
    } else {
      ElMessage.warning(`${node.name} 域内无 Active 设备`)
    }
  } else if (node.id !== srcDomain.value) {
    // 已选源: 选目标域
    form.target_domain_id = node.id
    ElMessage.success(`已选目标域: ${node.name}`)
  }
}

function selectSrcDevice(dev) {
  if (dev.status !== 'Active') {
    ElMessage.warning('只能选择 Active 状态的设备')
    return
  }
  form.src_did = dev.did
  ElMessage.success(`已选源设备: ${dev.device_id}`)
}

/* ───── 基础计算 ───── */
const src = computed(() => devices.value.find(d => d.did === form.src_did))
const srcDomain = computed(() => src.value?.domain_id || '')
const groupedDevices = computed(() => {
  const map = {}
  devices.value.filter(d => d.status === 'Active').forEach(d => { (map[d.domain_id] ||= []).push(d) })
  return Object.keys(map).map(k => ({ domain: k, list: map[k] }))
})
const targetDomains = computed(() => domains.value.filter(d => d.domain_id !== srcDomain.value))
const srcRuntimeHealth = computed(() => {
  if (!form.src_did) return ''
  if (!srcRT.value) return 'missing'
  const ageSec = Math.floor((Date.now() - new Date(srcRT.value.reported_at).getTime()) / 1000)
  return ageSec <= 300 ? 'fresh' : 'stale'
})
const controlExecList = computed(() => {
  return records.value.filter(r => r.action === 'exec').map(r => {
    const response = parseJSON(r.response)
    const req = parseJSON(r.request)
    return { ...r, command: req?.command || response?.applied_params?.command || '', priority: req?.priority ?? response?.applied_params?.priority, ok: r.status === 'success' && response?.executed !== false }
  })
})
const storageWriteList = computed(() => {
  return records.value.filter(r => r.action === 'write').map(r => {
    const response = parseJSON(r.response)
    const req = parseJSON(r.request)
    return { ...r, key: req?.key || response?.last_write?.params?.key || '', value: req?.value || response?.last_write?.params?.value || '', ok: r.status === 'success' && response?.write_ok !== false }
  })
})
const moduleGroups = computed(() => {
  const readonly = resources.value.filter(r => r.resource_type === 'sensor_data' || r.resource_type === 'query')
  const control = resources.value.filter(r => r.resource_type === 'control')
  const storage = resources.value.filter(r => r.resource_type === 'storage')
  return [
    { key: 'readonly', title: '只读模块', desc: '传感与查询资源（READ）', list: readonly },
    { key: 'control', title: '控制模块', desc: '执行控制命令（EXEC）', list: control },
    { key: 'storage', title: '存储模块', desc: '存储读写资源（READ / WRITE）', list: storage }
  ]
})

/* ───── 辅助函数 ───── */
function checkState(n) {
  if (step.value < 2) return ''
  if (!result.value) return 'pending'
  if (result.value.granted) return 'pass'
  const r = result.value.reason || ''
  const failStep = r.includes('not registered') || r.includes('revoked') || r.includes('not active') ? 1
                 : r.includes('Same domain') ? 2
                 : r.includes('Runtime') || r.includes('runtime') ? 3
                 : r.includes('offline') || r.includes('Signal') ? 4 : 0
  if (failStep === 0) return 'pass'
  if (n < failStep) return 'pass'
  if (n === failStep) return 'fail'
  return 'skip'
}
function scopeLabel(s) { return { readonly: '只读', control: '控制', storage: '存储' }[s] || s }
function scopeTagType(s) { return { readonly: 'success', control: 'warning', storage: '' }[s] || 'info' }
function approvalText(s) { return { not_required: '无需审批', pending: '待审批', approved: '已批准', rejected: '已驳回' }[s] || s }
function approvalTagType(s) { return { not_required: 'info', pending: 'warning', approved: 'success', rejected: 'danger' }[s] || 'info' }
function eventText(s) { return { init: '创建', approve: '批准', reject: '驳回' }[s] || s }
function eventTagType(s) { return { init: 'info', approve: 'success', reject: 'danger' }[s] || 'info' }
function formatDateTime(v) { return v ? new Date(v).toLocaleString() : '-' }
function resIcon(t) { return { sensor_data:'🌡️', control:'🎛️', query:'📊', storage:'💾' }[t] || '🔧' }
function resTypeLabel(t) { return { sensor_data:'传感数据', control:'控制', query:'查询', storage:'存储' }[t] || t }
function resTagType(t) { return { sensor_data:'success', control:'warning', query:'info', storage:'' }[t] || '' }
function recTagType(s) { return { success:'success', denied:'danger', expired:'info' }[s] || '' }
function recLabel(s) { return { success:'成功', denied:'拒绝', expired:'过期' }[s] || s }
function formatTime(t) { return new Date(t).toLocaleTimeString() }
function prettyJson(s) { try { return JSON.stringify(JSON.parse(s), null, 2) } catch(_) { return s } }
function parseJSON(s) { try { return JSON.parse(s || '{}') } catch(_) { return {} } }
function riskTagType(level) { return { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }[level] || 'info' }
function pfStatusText(s) { return { pass: '通过', fail: '未通过', warn: '预警', skip: '跳过' }[s] || s }
function timelineIcon(k) { return { auth: '🔐', approval: '✍️', access: '📡', revoke: '⛔' }[k] || '•' }
function timelineStatusTag(s) { return { success: 'success', denied: 'danger', expired: 'warning', pending: 'warning', info: 'info' }[s] || 'info' }
function timelineStatusText(s) { return { success: '成功', denied: '拒绝', expired: '过期', pending: '待处理', info: '信息' }[s] || s }
function shortDetail(d) { const txt = typeof d === 'string' ? d : JSON.stringify(d); return txt.length > 120 ? txt.slice(0, 120) + '…' : txt }
function actionLabel(a) { return { read: '执行读取', write: '执行写入', exec: '执行控制' }[a] || '调用接口' }
function formatAgo(ts) {
  const sec = Math.floor((Date.now() - new Date(ts).getTime()) / 1000)
  if (sec < 60) return `${sec}s 前`
  if (sec < 3600) return `${Math.floor(sec / 60)} 分钟前`
  return `${Math.floor(sec / 3600)} 小时前`
}

function tryRepairUtf8(str) {
  if (typeof str !== 'string' || !str) return str
  if (!/[ÃÂâæåçé]/.test(str)) return str
  try {
    const bytes = Uint8Array.from([...str].map(ch => ch.charCodeAt(0) & 0xff))
    const fixed = new TextDecoder('utf-8').decode(bytes)
    return /[\u4e00-\u9fa5]/.test(fixed) ? fixed : str
  } catch (_) { return str }
}
function showText(v) { return tryRepairUtf8(v || '') }
function sanitizeText(v) { return showText(v).replace(/\uFFFD/g, '').replace(/[\u0000-\u001f]/g, '').trim() }
function isGarbledText(v) {
  if (!v) return true
  if (v.includes('�')) return true
  const badChars = (v.match(/[^\u4e00-\u9fa5a-zA-Z0-9\s\-_.:()/%]/g) || []).length
  return badChars > Math.max(3, Math.floor(v.length * 0.35))
}
function parseMockData(r) { try { return JSON.parse(r?.mock_data || '{}') } catch(_) { return {} } }
function displayResName(r) {
  const raw = sanitizeText(r?.name || '')
  if (raw && !isGarbledText(raw)) return raw
  const md = parseMockData(r)
  const domain = sanitizeText(r?.domain_id || md.domain_id || result.value?.target_domain_id || '目标域')
  const device = sanitizeText(md.device_id || '设备')
  if (r?.resource_type === 'sensor_data') return `${domain}-${device}-实时遥测`
  if (r?.resource_type === 'control') return `${domain}-${device}-控制终端`
  if (r?.resource_type === 'query') return `${domain}-域态势看板`
  if (r?.resource_type === 'storage') return `${domain}-动态存储池`
  return `${domain}-业务资源`
}
function displayResDesc(r) {
  const raw = sanitizeText(r?.description || '')
  if (raw && !isGarbledText(raw)) return raw
  if (r?.resource_type === 'sensor_data') return '读取设备在线状态、信号强度与地理位置等只读遥测数据'
  if (r?.resource_type === 'control') return '向目标域设备下发控制命令（需要控制权限与审批）'
  if (r?.resource_type === 'query') return '查询目标域实时运行态势与统计汇总'
  if (r?.resource_type === 'storage') return '对目标域存储池进行键值读取与写入'
  return '目标域动态业务资源'
}
function resourceAction(r) {
  if (r._selectedAction) return r._selectedAction
  const acts = permissions.value?.resource_actions?.[r.id] || []
  if (acts.includes('exec')) return 'exec'
  if (acts.includes('write')) return 'write'
  if (acts.includes('read')) return 'read'
  return ''
}
function buildActionParams(r, act) {
  if (r.resource_type === 'control' || act === 'exec') return { command: r._ctrlCommand || 'reboot', priority: r._ctrlPriority || 3 }
  if (r.resource_type === 'storage') return { key: (r._storeKey || '').trim(), value: act === 'write' ? r._storeValue : undefined }
  return {}
}

/* ───── 场景 ───── */
async function loadScenarios() {
  try { const r = await api.crossAuthScenarios(); if (r.code === 0) scenarios.value = r.data || [] } catch (_) {}
}
function applyScenario(s) {
  selectedScenario.value = s.key
  form.target_domain_id = s.target_domain_id
  form.requested_scopes = [...s.requested_scopes]
  if (s.suggested_src) form.src_did = s.suggested_src.did
  preflight.value = null
  ElMessage.success(`已应用场景: ${s.title}`)
}

/* ───── 预评估 ───── */
async function runPreflight() {
  if (!form.src_did || !form.target_domain_id) return
  preflightLoading.value = true
  try {
    const r = await api.crossAuthPreflight({ src_did: form.src_did, target_domain_id: form.target_domain_id })
    if (r.code === 0) {
      preflight.value = r.data
      if (r.data.predict_granted) ElMessage.success('预评估: 预计可通过')
      else ElMessage.warning('预评估: ' + (r.data.predict_reason || '存在风险'))
    } else ElMessage.error(r.msg)
  } catch (e) { ElMessage.error(e.message) }
  finally { preflightLoading.value = false }
}

/* ───── 核心: 发起跨域认证 ───── */
async function submit() {
  loading.value = true; result.value = null; session.value = null; permissions.value = null
  approval.value = null; approvalHistory.value = []; resources.value = []; records.value = []; timeline.value = []
  lastAuthResult.value = null
  stopLivePolling()

  // 拓扑动画
  authAnimating.value = true
  authAnimStep.value = 1
  step.value = 1
  await sleep(500)
  authAnimStep.value = 2
  step.value = 2

  try {
    const r = await api.crossAuth(form)
    authAnimStep.value = 3
    await sleep(400)
    authAnimStep.value = 4
    step.value = 4
    if (r.code === 0) {
      result.value = r.data
      lastAuthResult.value = r.data
      setTimeout(() => step.value = 3, 300)
      if (r.data.granted) {
        ElMessage.success('认证通过, 可访问目标域资源')
        await loadSession()
        await loadApproval()
        await loadPermissions()
        await loadResources(r.data.target_domain_id)
        await loadTimeline()
        startLivePolling()
      } else {
        ElMessage.warning(r.data.reason)
      }
    } else ElMessage.error(r.msg)
    await load()
    await onSrcChange()
  } catch(e) { ElMessage.error(e.message) }
  finally {
    loading.value = false
    setTimeout(() => { authAnimating.value = false }, 2000)
  }
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)) }

/* ───── 会话 & 权限 ───── */
async function revokeCurrentSession() {
  if (!result.value?.session_id) return
  try {
    const r = await api.revokeSession(result.value.session_id, '目标域管理员主动撤销')
    if (r.code === 0) { ElMessage.success('会话已撤销'); await loadSession(); await loadTimeline() }
    else ElMessage.error(r.msg)
  } catch (e) { ElMessage.error(e.message) }
}
async function loadSession() {
  if (!result.value) return
  try { const r = await api.sessionInfo(result.value.session_id); if (r.code === 0) session.value = r.data } catch(_) {}
}
async function loadPermissions() {
  if (!result.value) return
  try { const r = await api.sessionPermissions(result.value.session_id); if (r.code === 0) permissions.value = r.data } catch (_) {}
}
async function loadApproval() {
  if (!result.value) return
  try { const r = await api.sessionApproval(result.value.session_id); if (r.code === 0) approval.value = r.data } catch (_) {}
  try { const h = await api.sessionApprovalHistory(result.value.session_id); if (h.code === 0) approvalHistory.value = h.data || [] } catch (_) {}
}
async function approveCurrentSession() {
  if (!result.value) return
  const r = await api.approveSession(result.value.session_id, '目标域管理员同意高权限')
  if (r.code === 0) { approval.value = r.data; ElMessage.success('高权限已批准'); await loadPermissions(); await loadApproval() }
  else ElMessage.error(r.msg)
}
async function rejectCurrentSession() {
  if (!result.value) return
  const r = await api.rejectSession(result.value.session_id, '目标域管理员拒绝高权限')
  if (r.code === 0) { approval.value = r.data; ElMessage.warning('高权限已驳回'); await loadPermissions(); await loadApproval() }
  else ElMessage.error(r.msg)
}
async function loadTimeline() {
  if (!result.value?.session_id) return
  try { const r = await api.sessionTimeline(result.value.session_id); if (r.code === 0) timeline.value = r.data || [] } catch (_) {}
}

/* ───── 资源 ───── */
async function loadResources(domainId) {
  const r = await api.listResources(domainId)
  if (r.code===0) {
    resources.value = r.data.map(x => ({
      ...x, name: showText(x.name), description: showText(x.description),
      _loading:false, _liveText:'', _liveAt:'', _selectedAction:'',
      _ctrlCommand:'reboot', _ctrlPriority:3, _storeKey:'', _storeValue:'',
      _metaOnline:null, _metaSignal:null, _metaGeo:'',
      _metaActive:null, _metaOnlineCount:null, _metaAvgSignal:null
    }))
    for (const rr of resources.value) {
      const md = parseMockData(rr)
      if (rr.resource_type === 'sensor_data') { rr._metaOnline = md.runtime?.online ?? null; rr._metaSignal = md.runtime?.signal_dbm ?? null; rr._metaGeo = md.runtime?.geo_hash || '' }
      if (rr.resource_type === 'query') { rr._metaActive = md.active_devices ?? null; rr._metaOnlineCount = md.online_devices ?? null; rr._metaAvgSignal = md.avg_signal_dbm ?? null }
    }
  }
}
async function refreshLiveData() {
  if (!result.value?.session_id || !resources.value.length) return
  liveRefreshing.value = true
  try {
    for (const r of resources.value) {
      if (r.resource_type === 'control') continue
      const rep = await api.accessResource({ session_id: result.value.session_id, resource_id: r.id, action: 'read', params: { preview: true } })
      if (rep.code === 0) { r._liveText = prettyJson(rep.data.response); r._liveAt = new Date().toISOString() }
    }
  } finally { liveRefreshing.value = false }
}
function startLivePolling() {
  stopLivePolling(); refreshLiveData()
  liveTimer = setInterval(() => { if (session.value?.remaining_sec > 0) refreshLiveData() }, 6000)
}
function stopLivePolling() { if (liveTimer) { clearInterval(liveTimer); liveTimer = null } }
async function loadRecords() {
  if (!result.value) return
  const r = await api.accessRecords(result.value.session_id); if (r.code===0) records.value = r.data
  await loadTimeline()
}
async function access(r) {
  const act = resourceAction(r)
  if (!act) { ElMessage.warning('当前会话对该资源无可用权限'); return }
  if (r.resource_type === 'storage') {
    const k = (r._storeKey || '').trim()
    if (!k) { ElMessage.warning('存储资源请先填写 Key'); return }
    if (act === 'write' && (r._storeValue === undefined || r._storeValue === null || `${r._storeValue}`.trim() === '')) {
      ElMessage.warning('WRITE 操作请填写 Value'); return
    }
  }
  r._loading = true
  try {
    const rep = await api.accessResource({ session_id: result.value.session_id, resource_id: r.id, action: act, params: buildActionParams(r, act) })
    if (rep.code === 0) { ElMessage.success('调用成功'); await loadRecords() }
    else { ElMessage.error(rep.msg); await loadRecords() }
  } catch(e) { ElMessage.error(e.response?.data?.msg || e.message); await loadRecords() }
  finally { r._loading = false }
}

/* ───── 数据加载 ───── */
async function load() {
  const r = await api.listDevices(''); if (r.code===0) devices.value = r.data
  const d = await api.listDomains(); if (d.code===0) domains.value = d.data
  await loadOracleStatus()
  await loadScenarios()
  const l = await api.authLogs()
  if (l.code===0) { stats.total = l.data.length; stats.granted = l.data.filter(x => x.granted).length }
}
async function onSrcChange() {
  srcRT.value = null
  if (!form.src_did) return
  await loadOracleStatus()
  try { const r = await api.runtime(form.src_did); if (r.code===0) srcRT.value = r.data } catch(_) {}
}
async function loadOracleStatus() {
  try { const r = await api.oracleStatus(); if (r.code === 0) Object.assign(oracleStatus, r.data) } catch (_) {}
}

/* ───── 生命周期 ───── */
onMounted(() => {
  load()
  oracleStatusTimer = setInterval(loadOracleStatus, 5000)
  sessionTimer = setInterval(() => {
    if (session.value && session.value.remaining_sec > 0) session.value.remaining_sec--
  }, 1000)
})
onUnmounted(() => {
  clearInterval(sessionTimer)
  clearInterval(oracleStatusTimer)
  stopLivePolling()
})
watch(() => form.src_did, onSrcChange)
</script>

<style scoped>
.ca-page { padding: 0; }

/* ═══ HERO ═══ */
.hero { background: linear-gradient(135deg,#667eea 0%,#764ba2 100%);
  color:#fff; border-radius:14px; padding:28px 34px;
  display:flex; justify-content:space-between; align-items:center; gap:24px;
  box-shadow:0 12px 40px rgba(102,126,234,.35); margin-bottom:20px; flex-wrap:wrap; }
.hero-text { flex: 1; min-width: 320px; }
.hero h2 { margin:10px 0 8px; font-size:24px; letter-spacing:1px; }
.hero p { margin:0; opacity:.92; line-height:1.7; max-width:640px; font-size:13.5px; }
.tag { display:inline-block; background:rgba(255,255,255,.22); padding:4px 14px; border-radius:20px; font-size:12px; backdrop-filter:blur(4px); }
.stat-box { display:flex; align-items:center; gap:18px; background:rgba(0,0,0,.18); padding:14px 22px; border-radius:12px; flex-shrink:0; backdrop-filter:blur(8px); }
.stat-box .st { text-align:center; min-width:50px; }
.stat-box .n { font-size:24px; font-weight:800; line-height:1; }
.stat-box .ok { color:#b7f7c8 }
.stat-box .fail { color:#ffd5d5 }
.stat-box .l { font-size:11px; opacity:.85; margin-top:4px; }
.stat-box .sep { width:1px; height:34px; background:rgba(255,255,255,.3); }

/* ═══ 通用面板 ═══ */
.panel { border-radius:12px; border:1px solid #ebeef5; margin-bottom:18px; }
.hdr { font-size:15px; font-weight:600; color:#303133; }
.hdr small { font-weight:400; color:#909399; margin-left:8px; }
.hdr-row { display:flex; justify-content:space-between; align-items:center; }
.muted { color:#909399; }

/* ═══ 拓扑视图 ═══ */
.topo-container { display:flex; gap:20px; align-items:flex-start; }
.topo-svg { flex:1; min-height:420px; border:1px solid #f0f2f5; border-radius:10px; background:linear-gradient(180deg, #fafbfc 0%, #f5f6f8 100%); }
.topo-info { width:200px; flex-shrink:0; }
.topo-legend { background:#fafcff; border:1px solid #e6edf8; border-radius:8px; padding:12px; margin-bottom:12px; }
.legend-title { font-size:12px; font-weight:600; color:#303133; margin-bottom:8px; }
.legend-item { font-size:11px; color:#606266; display:flex; align-items:center; gap:6px; margin:4px 0; }
.legend-item .dot { width:10px; height:10px; border-radius:50%; flex-shrink:0; }
.legend-item .line-sample { display:inline-block; width:20px; height:2px; flex-shrink:0; }
.legend-item .line-sample.solid { background:#67C23A; }
.legend-item .line-sample.dashed { background:repeating-linear-gradient(90deg, #409EFF 0, #409EFF 4px, transparent 4px, transparent 8px); height:2px; }
.topo-tip { background:#f4f4f5; border:1px dashed #dcdfe6; border-radius:8px; padding:10px 12px; font-size:12px; color:#606266; line-height:1.6; }
.topo-tip b { color:#303133; }
.topo-tip code { background:#eef; padding:1px 5px; border-radius:3px; font-size:11px; }
.topo-tip.active { background:#f0f9eb; border-color:#c2e7b0; }
.topo-tip.ready { background:#ecf5ff; border-color:#b3d8ff; }

.domain-node { cursor:pointer; transition: all .3s; }
.domain-node:hover circle:first-child { stroke-width:2.5; stroke:#1890ff; }
.domain-node.selected-src circle:first-child { stroke:#1890ff !important; stroke-width:3; filter:url(#glow); }
.domain-node.selected-tgt circle:first-child { stroke:#E6A23C !important; stroke-width:3; stroke-dasharray:6 3; }
.domain-node.auth-success circle:first-child { stroke:#52c41a !important; stroke-width:3; }
.domain-node.auth-fail circle:first-child { stroke:#ff4d4f !important; stroke-width:3; }

.device-dot { cursor:pointer; transition: all .2s; }
.device-dot:hover { transform: scale(1.2); }
.device-dot.dev-selected circle { stroke:#1890ff !important; stroke-width:3; }

.anim-pulse { animation: pulse 1.5s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity:1; } 50% { opacity:.6; } }
.anim-highlight { animation: highlight 2s ease-in-out infinite; }
@keyframes highlight { 0%,100% { stroke-width:1.5; } 50% { stroke-width:3.5; } }
.anim-dash { animation: dashMove 1s linear infinite; }
@keyframes dashMove { to { stroke-dashoffset: -24; } }

/* ═══ 对比面板 ═══ */
.compare-grid { display:grid; grid-template-columns:1fr 50px 1fr; gap:0; align-items:stretch; }
.compare-col { border:1px solid #ebeef5; border-radius:12px; padding:0; overflow:hidden; }
.compare-col.traditional { border-color:#ffd5d5; }
.compare-col.blockchain { border-color:#b7f7c8; }
.compare-head { padding:14px 18px; display:flex; align-items:center; gap:10px; }
.compare-col.traditional .compare-head { background:linear-gradient(135deg,#fff5f5,#ffe8e8); }
.compare-col.blockchain .compare-head { background:linear-gradient(135deg,#f0fff4,#e6ffe6); }
.compare-icon { font-size:24px; }
.compare-title { font-size:15px; font-weight:700; color:#303133; }
.compare-body { padding:12px 18px; }
.compare-item { display:flex; gap:10px; align-items:flex-start; padding:8px 0; border-bottom:1px dashed #f0f0f0; }
.compare-item:last-child { border-bottom:none; }
.ci-icon { font-size:16px; flex-shrink:0; margin-top:2px; }
.ci-label { font-size:13px; font-weight:600; color:#303133; }
.ci-desc { font-size:11px; color:#909399; margin-top:2px; line-height:1.5; }
.compare-vs { display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:900; color:#409EFF;
  background:linear-gradient(180deg,#ecf5ff,#e6f7ff); border-radius:50%; width:42px; height:42px; margin:auto; }
.compare-flow { padding:12px 18px; background:#fafafa; border-top:1px solid #f0f0f0; display:flex; align-items:center; justify-content:center; gap:8px; flex-wrap:wrap; }
.cf-step { background:#fff; border:1px solid #dcdfe6; padding:6px 14px; border-radius:8px; font-size:12px; font-weight:600; }
.cf-step.highlight-bad { border-color:#F56C6C; color:#F56C6C; background:#fef0f0; }
.cf-step.highlight-good { border-color:#67C23A; color:#67C23A; background:#f0f9eb; }
.cf-arrow { color:#909399; font-size:16px; font-weight:600; }
.cf-arrow.green { color:#67C23A; }
.cf-oracle { font-size:11px; color:#E6A23C; margin-left:4px; }

/* ═══ 流程可视化 (增强) ═══ */
.flow-enhanced { display:grid; grid-template-columns:200px 90px minmax(240px,1fr) 90px 200px; align-items:stretch; padding:20px 0 10px; gap:4px; }
.flow-node { background:#fff; border:2px solid #dcdfe6; border-radius:16px; padding:18px 14px; text-align:center; transition:all .4s; display:flex; flex-direction:column; align-items:center; }
.flow-node.active { border-color:#409EFF; box-shadow:0 0 24px rgba(64,158,255,.2); transform:translateY(-2px); }
.flow-node.active.src { border-color:#67C23A; box-shadow:0 0 24px rgba(103,194,58,.2); }
.flow-node.active.target { border-color:#67C23A; box-shadow:0 0 24px rgba(103,194,58,.2); }
.flow-node.fail { border-color:#F56C6C; box-shadow:0 0 24px rgba(245,108,108,.2); }
.fn-icon { font-size:36px; margin-bottom:6px; }
.fn-title { font-weight:700; font-size:14px; color:#303133; }
.fn-sub { font-size:12px; color:#909399; margin-top:2px; }
.fn-device { margin-top:8px; background:#f8fafc; border:1px solid #ebeef5; border-radius:6px; padding:6px 10px; }
.fn-dev-id { font-size:12px; font-weight:600; color:#303133; }
.fn-did { display:block; font-size:10px; color:#409EFF; margin-top:2px; }
.fn-runtime { display:flex; gap:8px; align-items:center; margin-top:6px; }
.fn-signal { font-size:11px; color:#606266; }
.fn-access-badge { margin-top:8px; }
.check-list { margin-top:10px; text-align:left; width:100%; border-top:1px dashed #ebeef5; padding-top:8px; }
.check-item { font-size:11px; color:#909399; padding:3px 0; display:flex; align-items:center; gap:5px; }
.check-item .ck-dot { width:8px; height:8px; border-radius:50%; background:#dcdfe6; flex-shrink:0; transition:all .3s; }
.check-item .ck-num { color:#c0c4cc; font-weight:600; font-size:10px; }
.check-item .ck-text { flex:1; }
.check-item .ck-detail { font-size:10px; color:#c0c4cc; }
.check-item.pending .ck-dot { background:#E6A23C; animation:blink 1s infinite; }
.check-item.pass .ck-dot { background:#67C23A; }
.check-item.pass { color:#67C23A; }
.check-item.fail .ck-dot { background:#F56C6C; }
.check-item.fail { color:#F56C6C; font-weight:600; }
.check-item.skip { opacity:.4; }
@keyframes blink { 50%{opacity:.3} }

.flow-connector { position:relative; display:flex; align-items:center; justify-content:center; }
.fc-line { position:absolute; left:0; right:0; top:50%; height:2px; background:#dcdfe6; z-index:0; transition:all .4s; }
.flow-connector.active .fc-line { background:linear-gradient(90deg,#67C23A,#409EFF); box-shadow:0 0 8px rgba(64,158,255,.4); }
.fc-line.green { background:linear-gradient(90deg,#67C23A,#52c41a) !important; }
.fc-line.red { background:linear-gradient(90deg,#F56C6C,#ff4d4f) !important; }
.fc-label { background:#fff; padding:3px 10px; font-size:11px; color:#909399; position:relative; z-index:1;
  border:1px solid #dcdfe6; border-radius:12px; white-space:nowrap; }
.flow-connector.active .fc-label { color:#409EFF; border-color:#409EFF; }
.fc-particles { position:absolute; left:0; right:0; top:50%; height:4px; overflow:hidden; }
.particle { position:absolute; width:6px; height:6px; border-radius:50%; background:#409EFF; animation: moveParticle 1.5s linear infinite; }
.particle.p2 { animation-delay: .5s; }
.particle.p3 { animation-delay: 1s; }
.particle.green { background:#67C23A; }
@keyframes moveParticle { 0%{left:0; opacity:1;} 100%{left:100%; opacity:0;} }

/* ═══ 预言机 ═══ */
.oracle-status-bar { margin-bottom:12px; padding:10px 12px; background:#f8fafd; border:1px solid #e6edf8; border-radius:8px; display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
.oracle-feed { margin-top:16px; background:linear-gradient(90deg,#f4f8fe,#eef6ff);
  border-left:4px solid #409EFF; padding:12px 16px; border-radius:6px;
  display:flex; align-items:center; gap:12px; font-size:13px; }
.o-icon { font-size:22px; }
.o-text code { background:#eef; padding:1px 6px; border-radius:3px; font-size:12px; }

/* ═══ 表单 ═══ */
.form-line { display:flex; gap:12px; align-items:center; justify-content:center; margin-top:20px; flex-wrap:wrap; }
.arr-icon { font-size:20px; color:#409EFF; }

/* ═══ 结果卡片 ═══ */
.result-card.r-ok { border-top:3px solid #67C23A; }
.result-card.r-fail { border-top:3px solid #F56C6C; }
.result-hd { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }
.result-hd .reason { color:#606266; font-size:13px; }
.countdown { margin-left:auto; background:#fff7e6; color:#E6A23C; padding:4px 12px; border-radius:12px;
  font-size:12px; border:1px solid #faecd8; }
.countdown b { color:#F56C6C; font-size:14px; }
.mono-wrap { font-family:Menlo,monospace; font-size:12px; word-break:break-all; white-space:normal !important; }

.chain-proof { margin-bottom:16px; }
.proof-title { font-size:13px; font-weight:600; color:#303133; margin-bottom:10px; display:flex; align-items:center; gap:6px; }

/* ═══ "如果没有跨域认证" 面板 ═══ */
.what-if-panel { margin-top:16px; background:linear-gradient(135deg,#fafcff,#f5f7fa); border:1px solid #e6edf8; border-radius:10px; padding:16px 18px; }
.wif-title { font-size:14px; font-weight:700; color:#303133; margin-bottom:14px; }
.wif-grid { display:grid; grid-template-columns:repeat(4, 1fr); gap:12px; }
.wif-item { background:#fff; border:1px solid #ebeef5; border-radius:8px; padding:12px; text-align:center; }
.wif-icon { font-size:24px; margin-bottom:6px; }
.wif-icon.bad { color:#F56C6C; }
.wif-icon.good { color:#67C23A; }
.wif-label { font-size:12px; font-weight:600; color:#303133; margin-bottom:4px; }
.wif-desc { font-size:11px; color:#909399; line-height:1.5; }

/* ═══ 资源卡片 ═══ */
.resources-card .perm-panel { margin-bottom:12px; padding:10px 12px; border:1px dashed #dbe5f5; border-radius:8px; background:#fcfdff; }
.perm-hd { display:flex; align-items:center; gap:10px; margin-bottom:8px; flex-wrap:wrap; }
.perm-scopes { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.module-block { margin-top:10px; }
.module-hd { display:flex; align-items:center; justify-content:space-between; margin-bottom:10px; }
.module-title { font-size:14px; font-weight:600; color:#303133; }
.approval-history { margin-top:10px; border-top:1px dashed #e5eaf3; padding-top:8px; }
.approval-event { display:flex; gap:10px; align-items:center; flex-wrap:wrap; font-size:12px; color:#606266; margin:4px 0; }
.spec-list { border:1px dashed #e6ebf5; border-radius:8px; background:#fff; padding:8px 10px; margin-bottom:10px; }
.spec-item { display:flex; gap:10px; align-items:center; flex-wrap:wrap; font-size:12px; color:#606266; padding:6px 0; border-bottom:1px dashed #f0f2f7; }
.spec-item:last-child { border-bottom:none; }

.res-card { background:#fff; border:1px solid #ebeef5; border-top:3px solid #409EFF; border-radius:8px;
  padding:14px; height:100%; display:flex; flex-direction:column; transition:all .2s; }
.res-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,.08); transform:translateY(-2px); }
.res-card.t-sensor_data { border-top-color:#67C23A; }
.res-card.t-control { border-top-color:#E6A23C; }
.res-card.t-query { border-top-color:#909399; }
.res-top { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:10px; }
.res-icon { font-size:32px; line-height:1; }
.res-name { font-size:14px; font-weight:600; color:#303133; margin-bottom:6px; }
.res-desc { font-size:12px; color:#909399; line-height:1.5; flex:1; margin-bottom:10px; }
.res-extra { display:flex; gap:6px; flex-wrap:wrap; margin-bottom:10px; }
.res-live { background:#f8fbff; border:1px solid #e6effa; border-radius:6px; padding:8px; margin-bottom:10px; }
.res-live-hd { display:flex; justify-content:space-between; align-items:center; font-size:11px; color:#409EFF; margin-bottom:6px; }
.res-live-time { color:#909399; }
.res-live-body { margin:0; font-size:11px; color:#606266; max-height:74px; overflow:auto; white-space:pre-wrap; word-break:break-all; }
.perm-actions { display:flex; gap:6px; flex-wrap:wrap; margin-bottom:8px; }

.rec-list { max-height:500px; overflow-y:auto; }
.rec { border-left:3px solid #dcdfe6; padding:10px 14px; margin-bottom:8px; background:#fafafa; border-radius:0 6px 6px 0; }
.rec.success { border-left-color:#67C23A; background:#f0f9eb; }
.rec.denied { border-left-color:#F56C6C; background:#fef0f0; }
.rec.expired { border-left-color:#E6A23C; background:#fdf6ec; }
.rec-hd { display:flex; align-items:center; gap:10px; margin-bottom:6px; font-size:13px; }
.rec-name { font-weight:600; color:#303133; }
.rec-time { margin-left:auto; color:#909399; font-size:11px; }
.rec-body { background:#fff; border:1px solid #ebeef5; padding:8px 10px; border-radius:4px;
  font-size:11px; color:#606266; max-height:120px; overflow:auto; margin:0; white-space:pre-wrap; word-break:break-all; }

/* ═══ 场景 ═══ */
.scenarios { display:grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap:12px; }
.scenario-card { border:1px solid #ebeef5; border-left:4px solid #409EFF; border-radius:8px;
  padding:12px 14px; background:#fafcff; cursor:pointer; transition:all .2s; display:flex; flex-direction:column; gap:8px; }
.scenario-card:hover { box-shadow:0 4px 12px rgba(64,158,255,.15); transform:translateY(-2px); }
.scenario-card.active { border-left-color:#67C23A; background:#f3faf0; box-shadow:0 0 0 2px rgba(103,194,58,.25); }
.sc-hd { display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.sc-title { font-size:14px; font-weight:600; color:#303133; flex:1; }
.sc-story { font-size:12px; color:#606266; line-height:1.6; }
.sc-flow { display:flex; align-items:center; gap:6px; flex-wrap:wrap; font-size:12px; }
.sc-hl { display:flex; gap:6px; flex-wrap:wrap; }

/* ═══ 预评估 ═══ */
.preflight-panel { margin-top:16px; border:1px solid #ebeef5; border-radius:8px; padding:12px 14px; background:#fcfdff; }
.preflight-panel.pf-ok { border-left:4px solid #67C23A; background:#f6fbf2; }
.preflight-panel.pf-fail { border-left:4px solid #F56C6C; background:#fdf5f5; }
.pf-hd { display:flex; align-items:center; gap:10px; flex-wrap:wrap; margin-bottom:10px; }
.pf-checks { display:grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap:10px; }
.pf-chk { display:flex; gap:10px; padding:8px 10px; border-radius:6px; background:#fff; border:1px solid #ebeef5; }
.pf-dot { width:10px; height:10px; border-radius:50%; background:#dcdfe6; flex-shrink:0; margin-top:4px; }
.pf-chk.s-pass .pf-dot { background:#67C23A; }
.pf-chk.s-fail .pf-dot { background:#F56C6C; }
.pf-chk.s-warn .pf-dot { background:#E6A23C; }
.pf-chk.s-skip .pf-dot { background:#c0c4cc; }
.pf-chk.s-fail { background:#fef0f0; border-color:#fbc4c4; }
.pf-chk.s-warn { background:#fdf6ec; border-color:#f5dab1; }
.pf-body { flex:1; font-size:12px; }
.pf-label { font-weight:600; color:#303133; display:flex; gap:6px; align-items:center; }
.pf-status { font-style:normal; font-size:11px; color:#909399; font-weight:400; }
.pf-chk.s-pass .pf-status { color:#67C23A; }
.pf-chk.s-fail .pf-status { color:#F56C6C; }
.pf-chk.s-warn .pf-status { color:#E6A23C; }
.pf-detail { font-size:11px; color:#606266; margin-top:4px; line-height:1.5; }
.pf-hints { margin-top:10px; padding:8px 12px; background:#fff8e1; border-left:3px solid #E6A23C; border-radius:4px; font-size:12px; color:#7c5b00; }
.pf-hints ul { margin:4px 0 0 16px; padding:0; }

/* ═══ 时间线 ═══ */
.timeline { position:relative; padding-left:6px; }
.timeline::before { content:''; position:absolute; left:18px; top:0; bottom:0; width:2px; background:#ebeef5; }
.tl-item { display:flex; gap:12px; align-items:flex-start; padding:10px 0; position:relative; }
.tl-marker { width:30px; height:30px; border-radius:50%; background:#fff; border:2px solid #dcdfe6;
  display:flex; align-items:center; justify-content:center; font-size:14px; z-index:1; flex-shrink:0; }
.tl-item.k-auth .tl-marker { border-color:#409EFF; background:#ecf5ff; }
.tl-item.k-approval .tl-marker { border-color:#E6A23C; background:#fdf6ec; }
.tl-item.k-access .tl-marker { border-color:#67C23A; background:#f0f9eb; }
.tl-item.k-revoke .tl-marker { border-color:#F56C6C; background:#fef0f0; }
.tl-item.s-denied .tl-marker { border-color:#F56C6C; }
.tl-body { flex:1; background:#fff; border:1px solid #ebeef5; border-radius:6px; padding:8px 12px; }
.tl-hd { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
.tl-title { font-weight:600; color:#303133; font-size:13px; }
.tl-time { margin-left:auto; color:#909399; font-size:11px; }
.tl-detail { font-size:12px; color:#606266; margin-top:4px; display:flex; gap:10px; flex-wrap:wrap; }
.tl-note { color:#606266; font-size:12px; }
.tl-extra { margin-top:6px; display:flex; gap:8px; align-items:center; flex-wrap:wrap; font-size:12px; }
</style>
