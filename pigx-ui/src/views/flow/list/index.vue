<script setup lang="ts" name="flowList">
import { computed, onMounted, reactive, ref, shallowRef, useTemplateRef } from 'vue';
import { Search } from '@element-plus/icons-vue';
import type { FlowVO } from '/@/api/flow/flow/types';
import { queryMineStartGroupFlowList } from '/@/api/flow/group';
import type { GroupVO } from '/@/api/flow/group/types';
import Start from '/@/views/flow/form/tools/startFlow.vue';
import FlowCatalog from './components/FlowCatalog.vue';
import type { FlowGroupView } from './types';

interface StartDrawerExpose {
	handle: (flow: FlowVO & { flowId: string }) => void;
}

const route = useRoute();
const startRef = useTemplateRef<StartDrawerExpose>('startRef');
const queryForm = reactive({ flowName: '' });
const successGroupList = ref<GroupVO[]>([]);
const activeGroupKey = shallowRef('all');
const activeKeyword = shallowRef('');
const loading = shallowRef(false);

const groupViews = computed<FlowGroupView[]>(() =>
	successGroupList.value.map((group, index) => ({
		key: String(group.id ?? group.name ?? group.groupName ?? index),
		name: group.name || group.groupName || '未分组',
		items: group.items || [],
	}))
);

const visibleGroups = computed<FlowGroupView[]>(() => {
	const keyword = activeKeyword.value.toLocaleLowerCase();
	const selectedGroups = activeGroupKey.value === 'all' ? groupViews.value : groupViews.value.filter((group) => group.key === activeGroupKey.value);

	return selectedGroups
		.map((group) => ({
			...group,
			items: keyword ? group.items.filter((flow) => `${flow.name || ''}${flow.formName || ''}`.toLocaleLowerCase().includes(keyword)) : group.items,
		}))
		.filter((group) => group.items.length > 0);
});

const loadFlowGroups = async () => {
	loading.value = true;
	try {
		const { data } = await queryMineStartGroupFlowList('0');
		successGroupList.value = data || [];
	} finally {
		loading.value = false;
	}
};

const handleQuery = () => {
	activeKeyword.value = queryForm.flowName.trim();
};

const resetQuery = () => {
	queryForm.flowName = '';
	activeKeyword.value = '';
};

const startProcess = (flow: FlowVO) => {
	if (!flow.flowId) return;
	startRef.value?.handle({ ...flow, flowId: flow.flowId });
};

onMounted(() => {
	loadFlowGroups();
	const flowId = route.query.flowId as string | undefined;
	if (flowId) {
		startProcess({ flowId });
	}
});
</script>

<template>
	<div class="layout-padding">
		<main class="layout-padding-auto layout-padding-view flow-page">
			<div class="flow-search" role="search">
				<el-input
					v-model="queryForm.flowName"
					class="flow-search__input"
					:prefix-icon="Search"
					placeholder="搜索流程名称"
					clearable
					@keyup.enter="handleQuery"
					@clear="resetQuery"
				/>
				<el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
			</div>

			<div v-loading="loading" class="flow-workspace">
				<el-tabs v-model="activeGroupKey" class="flow-tabs">
					<el-tab-pane label="全部流程" name="all" />
					<el-tab-pane v-for="group in groupViews" :key="group.key" :label="group.name" :name="group.key" />
				</el-tabs>
				<FlowCatalog :groups="visibleGroups" :keyword="activeKeyword" @start="startProcess" />
			</div>

			<Start ref="startRef" />
		</main>
	</div>
</template>

<style scoped>
.flow-page {
	display: flex;
	flex-direction: column;
	padding: 24px;
	overflow: hidden;
	background: var(--el-bg-color);
}

.flow-search {
	display: flex;
	flex: none;
	gap: 12px;
	align-items: center;
}

.flow-search__input {
	width: min(320px, calc(100% - 84px));
}

.flow-workspace {
	display: flex;
	flex: 1;
	flex-direction: column;
	min-height: 0;
	margin-top: 20px;
	overflow: hidden;
}

.flow-tabs {
	flex: none;
}

.flow-tabs :deep(.el-tabs__header) {
	margin-bottom: 16px;
}

.flow-tabs :deep(.el-tabs__content) {
	display: none;
}

@media (max-width: 900px) {
	.flow-page {
		padding: 18px;
		overflow-y: auto;
	}

	.flow-workspace {
		flex: none;
		min-height: auto;
		overflow: visible;
	}
}

@media (max-width: 520px) {
	.flow-search {
		align-items: stretch;
	}

	.flow-search__input {
		width: 100%;
	}
}
</style>
