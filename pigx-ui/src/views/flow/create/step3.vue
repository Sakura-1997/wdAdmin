<script setup lang="ts">
import { computed, nextTick, ref, useTemplateRef, watch } from 'vue';
import { useEventListener } from '@vueuse/core';
import { Aim, CircleCheck, RefreshRight, Warning, ZoomIn, ZoomOut } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useI18n } from 'vue-i18n';
import nodeWrap from '../workflow/components/nodeWrap.vue';
import approverDrawer from '../workflow/components/drawer/approverDrawer.vue';
import conditionDrawer from '../workflow/components/drawer/conditionDrawer.vue';
import copyerDrawer from '../workflow/components/drawer/copyerDrawer.vue';
import promoterDrawer from '../workflow/components/drawer/promoterDrawer.vue';
import timerDrawer from '../workflow/components/drawer/timerDrawer.vue';
import triggerDrawer from '../workflow/components/drawer/triggerDrawer.vue';
import { useFlowStore } from '../workflow/stores/flow';

interface FlowNode {
	id?: string;
	type?: number;
	nodeName?: string;
	error?: boolean;
	conditionType?: number;
	ruleName?: string;
	expressionText?: string;
	childNode?: FlowNode;
	conditionNodes?: FlowNode[];
	[key: string]: unknown;
}

interface Step3Props {
	nodeConfigObj?: FlowNode;
}

interface ScrollbarHandle {
	update?: () => void;
	setScrollTop?: (value: number) => void;
	setScrollLeft?: (value: number) => void;
}

const MIN_ZOOM = 50;
const MAX_ZOOM = 300;
const ZOOM_STEP = 10;

const props = defineProps<Step3Props>();
const store = useFlowStore();
const { t } = useI18n();

const scrollbarRef = useTemplateRef<ScrollbarHandle>('scrollbarRef');
const workspaceRef = useTemplateRef<HTMLElement>('workspaceRef');
const tipList = ref<string[]>([]);
const nowVal = ref(100);
const nodeConfig = ref<FlowNode>({
	nodeName: t('flow.initiator'),
	type: 0,
	id: 'root',
	formPerms: {},
	eventConfig: '',
	nodeUserList: [],
	childNode: {},
});

watch(
	() => props.nodeConfigObj,
	(value) => {
		if (value && Object.keys(value).length > 0) {
			nodeConfig.value = value;
		}
	}
);

watch(
	() => nodeConfig.value,
	(value) => store.setStep3(value),
	{ deep: true }
);

const collectNodeErrors = (currentNode: FlowNode | undefined, errors: string[]) => {
	if (!currentNode?.id) return;

	const { type, error, nodeName = '', conditionNodes = [] } = currentNode;
	if (type === 1 || type === 2) {
		if (error) errors.push(`${nodeName} 未设置人员`);
	}
	if (type === 10 && error) {
		errors.push(`${nodeName} 未配置延时规则`);
	}
	if (type === 11 && error) {
		errors.push(`${nodeName} 未配置触发规则`);
	}

	if (type === 4) {
		conditionNodes.forEach((conditionNode) => {
			if (conditionNode.conditionType === 1) {
				if (!conditionNode.ruleName?.trim()) errors.push(`${conditionNode.nodeName ?? ''} 请输入规则名称`);
				if (!conditionNode.expressionText?.trim()) errors.push(`${conditionNode.nodeName ?? ''} 请输入条件表达式`);
			} else if (conditionNode.error) {
				errors.push(`请设置${conditionNode.nodeName ?? ''}的条件`);
			}
			collectNodeErrors(conditionNode, errors);
		});
	} else if (type === 5) {
		conditionNodes.forEach((conditionNode) => collectNodeErrors(conditionNode, errors));
	}

	collectNodeErrors(currentNode.childNode, errors);
};

const getValidationErrors = () => {
	const errors: string[] = [];
	if (!nodeConfig.value.childNode?.id) errors.push('请完善流程节点');
	collectNodeErrors(nodeConfig.value.childNode, errors);
	return errors;
};

const countNodes = (currentNode?: FlowNode): number => {
	if (!currentNode?.id) return 0;
	const branchCount = currentNode.conditionNodes?.reduce((total, node) => total + countNodes(node), 0) ?? 0;
	return 1 + branchCount + countNodes(currentNode.childNode);
};

const nodeCount = computed(() => countNodes(nodeConfig.value));
const issueCount = computed(() => getValidationErrors().length);
const clampZoom = (value: number) => Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, value));

const zoomIn = () => {
	nowVal.value = clampZoom(nowVal.value + ZOOM_STEP);
};

const zoomOut = () => {
	nowVal.value = clampZoom(nowVal.value - ZOOM_STEP);
};

const resetZoom = () => {
	nowVal.value = 100;
};

const fitCanvas = async () => {
	resetZoom();
	await nextTick();
	scrollbarRef.value?.update?.();
	scrollbarRef.value?.setScrollTop?.(0);
	scrollbarRef.value?.setScrollLeft?.(0);
};

useEventListener(
	workspaceRef,
	'wheel',
	(event: WheelEvent) => {
		if (!event.ctrlKey && !event.metaKey) return;
		event.preventDefault();
		nowVal.value = clampZoom(nowVal.value + (event.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP));
	},
	{ passive: false }
);

const validate = async (): Promise<void> => {
	tipList.value = getValidationErrors();
	if (tipList.value.length) throw tipList.value;
};

const checkProcess = async () => {
	try {
		await validate();
		ElMessage.success('流程检查通过');
	} catch (errors) {
		const messages = Array.isArray(errors) ? errors : [String(errors)];
		ElMessage.warning(`还有 ${messages.length} 项待完善，请检查标红节点`);
	}
};

const getProcessData = async () => nodeConfig.value;

defineExpose({ validate, getProcessData });
</script>

<template>
	<div class="process-designer">
		<div class="process-toolbar" aria-label="流程设计工具栏">
			<div class="process-summary">
				<span class="summary-text">流程共 {{ nodeCount }} 个节点</span>
				<span class="summary-divider" aria-hidden="true"></span>
				<span class="status-chip" :class="{ 'is-ready': issueCount === 0 }">
					<el-icon><CircleCheck v-if="issueCount === 0" /><Warning v-else /></el-icon>
					{{ issueCount === 0 ? '流程完整' : `${issueCount} 项待完善` }}
				</span>
			</div>

			<div class="toolbar-actions">
				<el-button text :icon="CircleCheck" data-testid="flow-check" @click="checkProcess">流程检查</el-button>
				<span class="action-divider" aria-hidden="true"></span>
				<el-button text :icon="Aim" data-testid="flow-fit-canvas" @click="fitCanvas">适应画布</el-button>
			</div>
		</div>

		<div ref="workspaceRef" class="flow-workspace">
			<el-scrollbar ref="scrollbarRef" class="flow-design-area" always>
				<section class="dingflow-design" aria-label="流程画布">
					<div class="box-scale" :style="{ transform: `scale(${nowVal / 100})` }">
						<nodeWrap v-model:nodeConfig="nodeConfig" />
						<div class="end-node">
							<div class="end-node-circle"></div>
							<div class="end-node-text">{{ $t('flow.end') }}</div>
						</div>
					</div>
				</section>
			</el-scrollbar>

			<div class="zoom-toolbar" aria-label="画布缩放">
				<el-tooltip content="缩小" placement="top">
					<el-button :icon="ZoomOut" :disabled="nowVal <= MIN_ZOOM" aria-label="缩小画布" circle size="small" @click="zoomOut" />
				</el-tooltip>
				<span class="zoom-value">{{ nowVal }}%</span>
				<el-tooltip content="放大" placement="top">
					<el-button :icon="ZoomIn" :disabled="nowVal >= MAX_ZOOM" aria-label="放大画布" circle size="small" @click="zoomIn" />
				</el-tooltip>
				<span class="zoom-divider" aria-hidden="true"></span>
				<el-tooltip content="重置缩放" placement="top">
					<el-button :icon="RefreshRight" aria-label="重置画布缩放" circle size="small" @click="resetZoom" />
				</el-tooltip>
			</div>
		</div>

		<promoterDrawer />
		<approverDrawer />
		<copyerDrawer />
		<conditionDrawer />
		<timerDrawer />
		<triggerDrawer />
	</div>
</template>

<style scoped lang="scss">
@import '../workflow/css/workflow.css';

.process-designer {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-width: 0;
	height: 100%;
	min-height: 0;
	background: var(--el-bg-color-page);
}

.process-toolbar {
	z-index: 3;
	display: flex;
	flex: 0 0 auto;
	align-items: center;
	justify-content: space-between;
	min-height: 52px;
	padding: 8px 20px;
	background: var(--el-bg-color);
	border-bottom: 1px solid var(--el-border-color-lighter);
}

.process-summary,
.toolbar-actions,
.status-chip {
	display: flex;
	align-items: center;
}

.process-summary {
	gap: 12px;
	min-width: 0;
}

.summary-text {
	font-size: 13px;
	font-weight: 500;
	color: var(--el-text-color-primary);
	white-space: nowrap;
}

.summary-divider,
.action-divider,
.zoom-divider {
	display: inline-block;
	width: 1px;
	background: var(--el-border-color-light);
}

.summary-divider {
	height: 16px;
}

.status-chip {
	gap: 5px;
	font-size: 13px;
	color: var(--el-color-warning);
	white-space: nowrap;
}

.status-chip.is-ready {
	color: var(--el-color-success);
}

.toolbar-actions {
	gap: 4px;
}

.toolbar-actions :deep(.el-button) {
	margin-left: 0;
	color: var(--el-text-color-regular);
}

.toolbar-actions :deep(.el-button:hover),
.toolbar-actions :deep(.el-button:focus-visible) {
	color: var(--el-color-primary);
}

.action-divider {
	height: 20px;
	margin: 0 6px;
}

.flow-workspace {
	position: relative;
	flex: 1;
	min-width: 0;
	min-height: 0;
	overflow: hidden;
	background-color: var(--el-bg-color-page);
	background-image: radial-gradient(var(--el-border-color-light) 0.8px, transparent 0.8px);
	background-size: 16px 16px;
}

.flow-design-area {
	height: 100%;
}

.dingflow-design {
	box-sizing: border-box;
	min-width: 100%;
	min-height: 100%;
	overflow: visible;
}

.dingflow-design .box-scale {
	box-sizing: border-box;
	min-height: 100%;
	padding: 36px 52px 96px;
	transform-origin: 50% 0;
	transition: transform 160ms ease;
}

.zoom-toolbar {
	position: absolute;
	right: 20px;
	bottom: 18px;
	z-index: 4;
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 6px 8px;
	background: color-mix(in srgb, var(--el-bg-color) 94%, transparent);
	border: 1px solid var(--el-border-color-lighter);
	border-radius: 12px;
	box-shadow: var(--el-box-shadow-light);
	backdrop-filter: blur(8px);
}

.zoom-toolbar :deep(.el-button) {
	margin-left: 0;
}

.zoom-value {
	min-width: 42px;
	font-size: 13px;
	font-variant-numeric: tabular-nums;
	color: var(--el-text-color-regular);
	text-align: center;
}

.zoom-divider {
	height: 22px;
	margin: 0 2px;
}

:deep(.node-wrap-box),
:deep(.auto-judge) {
	color: var(--el-text-color-primary);
	background: var(--el-bg-color-overlay);
}

:deep(.node-wrap-box .content),
:deep(.auto-judge .content),
:deep(.end-node-text) {
	color: var(--el-text-color-regular);
}

:deep(.node-wrap-box::before) {
	background: var(--el-bg-color-page);
	border-top-color: var(--el-border-color);
}

:deep(.branch-box .col-box),
:deep(.top-left-cover-line),
:deep(.top-right-cover-line),
:deep(.bottom-left-cover-line),
:deep(.bottom-right-cover-line) {
	background-color: var(--el-bg-color-page);
}

:deep(.branch-box) {
	border-color: var(--el-border-color);
}

:deep(.branch-box .col-box::before),
:deep(.condition-node-box::before) {
	background-color: var(--el-border-color);
}

:deep(.add-branch) {
	color: var(--el-color-primary);
	background: var(--el-bg-color-overlay);
	border: 1px solid var(--el-border-color-lighter);
}

@media (max-width: 900px) {
	.process-toolbar {
		align-items: flex-start;
		gap: 8px;
		padding-inline: 14px;
	}

	.process-summary {
		flex-wrap: wrap;
		gap: 6px 10px;
	}

	.summary-divider,
	.action-divider {
		display: none;
	}

	.toolbar-actions :deep(.el-button) {
		padding-inline: 8px;
	}

	.dingflow-design .box-scale {
		padding-inline: 28px;
	}

	.zoom-toolbar {
		right: 12px;
		bottom: 12px;
	}
}

@media (prefers-reduced-motion: reduce) {
	.dingflow-design .box-scale {
		transition: none;
	}
}
</style>
