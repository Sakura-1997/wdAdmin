<script lang="ts">
export default {
	title: '工作流信息',
	icon: 'PieChart',
	description: '获取工作流信息展示',
};
</script>
<template>
	<el-card class="flow-card h-[191px]">
		<div class="flex h-full">
			<!-- 待办任务 -->
			<router-link to="/flow/task/pending" class="flex-1 flex flex-col items-center justify-center rounded-l-lg no-underline transition duration-200 hover:bg-fill-lighter hover:-translate-y-px">
				<p class="text-4xl font-bold tabular-nums text-gray-800 dark:text-gray-100">{{ state.pendingNum }}</p>
				<p class="mt-2 text-sm font-medium text-gray-500 dark:text-gray-400">{{ $t('home.pendingTask') }}</p>
			</router-link>
			<div class="w-px self-center h-12 bg-gray-200 dark:bg-gray-700"></div>
			<!-- 抄送任务 -->
			<router-link to="/flow/task/cc" class="flex-1 flex flex-col items-center justify-center rounded-r-lg no-underline transition duration-200 hover:bg-fill-lighter hover:-translate-y-px">
				<p class="text-4xl font-bold tabular-nums text-gray-800 dark:text-gray-100">{{ state.copyNum }}</p>
				<p class="mt-2 text-sm font-medium text-gray-500 dark:text-gray-400">{{ $t('home.copyTask') }}</p>
			</router-link>
		</div>
	</el-card>
</template>

<script setup lang="ts" name="flowData">
import { queryTaskData } from '/@/api/flow/task';
import { useAsyncState } from '@vueuse/core';

const { state } = useAsyncState(
	() => queryTaskData().then(({ data }) => ({
		pendingNum: Number.parseInt(data?.pendingNum || 0),
		copyNum: Number.parseInt(data?.copyNum || 0),
	})),
	{ pendingNum: 0, copyNum: 0 },
	// 静默错误：避免没有启动 flow 模块时 vue 组件渲染 warning
	{ onError: () => {} },
);
</script>
<style scoped>
.flow-card :deep(.el-card__body) {
	padding: 0;
	height: 100%;
}
</style>
