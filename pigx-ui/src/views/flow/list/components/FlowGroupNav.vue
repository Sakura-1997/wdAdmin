<script setup lang="ts">
import { CollectionTag } from '@element-plus/icons-vue';
import type { FlowGroupView } from '../types';

interface Props {
	groups: FlowGroupView[];
	activeKey: string;
	total: number;
}

defineProps<Props>();

const emit = defineEmits<{
	select: [key: string];
}>();
</script>

<template>
	<aside class="group-nav" aria-label="流程分组">
		<div class="group-nav__title">
			<el-icon><CollectionTag /></el-icon>
			<span>流程分组</span>
		</div>

		<nav class="group-nav__list">
			<button type="button" class="group-nav__item" :class="{ 'is-active': activeKey === 'all' }" @click="emit('select', 'all')">
				<span>全部流程</span>
				<span class="group-nav__count">{{ total }}</span>
			</button>
			<button
				v-for="group in groups"
				:key="group.key"
				type="button"
				class="group-nav__item"
				:class="{ 'is-active': activeKey === group.key }"
				@click="emit('select', group.key)"
			>
				<span class="group-nav__name">{{ group.name }}</span>
				<span class="group-nav__count">{{ group.items.length }}</span>
			</button>
		</nav>
	</aside>
</template>

<style scoped>
.group-nav {
	height: 100%;
	min-height: 0;
	padding: 16px 8px;
	overflow-y: auto;
	background: var(--el-bg-color);
	border: 1px solid var(--el-border-color-light);
	border-radius: 8px;
}

.group-nav__title {
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 0 10px 14px;
	font-size: 16px;
	font-weight: 600;
	color: var(--el-text-color-primary);
	border-bottom: 1px solid var(--el-border-color-lighter);
}

.group-nav__list {
	display: flex;
	flex-direction: column;
	gap: 4px;
	padding-top: 8px;
}

.group-nav__item {
	position: relative;
	display: flex;
	align-items: center;
	justify-content: space-between;
	width: 100%;
	height: 44px;
	padding: 0 12px;
	font: inherit;
	font-size: 14px;
	color: var(--el-text-color-regular);
	text-align: left;
	cursor: pointer;
	background: transparent;
	border: 0;
	border-radius: 6px;
	transition: color 0.2s ease, background-color 0.2s ease;
}

.group-nav__item:hover {
	color: var(--el-color-primary);
	background: var(--el-fill-color-light);
}

.group-nav__item.is-active {
	font-weight: 600;
	color: var(--el-color-primary);
	background: var(--el-color-primary-light-9);
}

.group-nav__item.is-active::before {
	position: absolute;
	top: 8px;
	bottom: 8px;
	left: -8px;
	width: 3px;
	content: '';
	background: var(--el-color-primary);
	border-radius: 0 3px 3px 0;
}

.group-nav__name {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.group-nav__count {
	margin-left: 12px;
	font-size: 13px;
	font-weight: 500;
	color: var(--el-text-color-secondary);
}

.group-nav__item.is-active .group-nav__count {
	color: var(--el-color-primary);
}

@media (max-width: 900px) {
	.group-nav {
		height: auto;
		min-height: auto;
		overflow: visible;
	}

	.group-nav__list {
		flex-flow: row wrap;
	}

	.group-nav__item {
		width: auto;
		min-width: 120px;
	}

	.group-nav__item.is-active::before {
		top: auto;
		right: 12px;
		bottom: 0;
		left: 12px;
		width: auto;
		height: 2px;
		border-radius: 2px 2px 0 0;
	}
}
</style>
