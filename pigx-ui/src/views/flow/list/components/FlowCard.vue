<script setup lang="ts">
import { computed, shallowRef } from 'vue';
import { Promotion, Right } from '@element-plus/icons-vue';
import type { FlowVO } from '/@/api/flow/flow/types';
import { baseURL } from '/@/utils/globalProperties';

const props = defineProps<{
	flow: FlowVO;
}>();

const emit = defineEmits<{
	start: [flow: FlowVO];
}>();

const imageFailed = shallowRef(false);
const flowName = computed(() => props.flow.name || props.flow.formName || '未命名流程');
const flowDescription = computed(() => props.flow.remark || '点击发起并填写流程申请');
const logoUrl = computed(() => {
	if (!props.flow.logo || imageFailed.value) return '';
	return /^https?:\/\//i.test(props.flow.logo) ? props.flow.logo : `${baseURL}${props.flow.logo}`;
});
</script>

<template>
	<button type="button" class="flow-card" @click="emit('start', flow)">
		<span class="flow-card__icon">
			<img v-if="logoUrl" :src="logoUrl" :alt="`${flowName}图标`" @error="imageFailed = true" />
			<el-icon v-else :size="24"><Promotion /></el-icon>
		</span>
		<span class="flow-card__content">
			<strong class="flow-card__name">{{ flowName }}</strong>
			<span class="flow-card__description">{{ flowDescription }}</span>
		</span>
		<span class="flow-card__action">
			立即发起
			<el-icon><Right /></el-icon>
		</span>
	</button>
</template>

<style scoped>
.flow-card {
	display: grid;
	grid-template-columns: 48px minmax(0, 1fr);
	grid-template-rows: auto 1fr auto;
	gap: 0 14px;
	width: 100%;
	min-height: 146px;
	padding: 20px;
	font: inherit;
	color: var(--el-text-color-primary);
	text-align: left;
	cursor: pointer;
	background: var(--el-bg-color);
	border: 1px solid var(--el-border-color-light);
	border-radius: 8px;
	transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.flow-card:hover,
.flow-card:focus-visible {
	border-color: var(--el-color-primary-light-5);
	box-shadow: 0 8px 20px rgb(31 84 195 / 10%);
	transform: translateY(-2px);
	outline: none;
}

.flow-card__icon {
	display: inline-flex;
	grid-row: 1 / span 2;
	align-items: center;
	justify-content: center;
	width: 48px;
	height: 48px;
	overflow: hidden;
	color: var(--el-color-primary);
	background: var(--el-color-primary-light-9);
	border: 1px solid var(--el-color-primary-light-8);
	border-radius: 10px;
}

.flow-card__icon img {
	width: 100%;
	height: 100%;
	object-fit: cover;
}

.flow-card__content {
	display: flex;
	flex-direction: column;
	min-width: 0;
}

.flow-card__name {
	overflow: hidden;
	font-size: 16px;
	font-weight: 600;
	line-height: 24px;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.flow-card__description {
	display: -webkit-box;
	margin-top: 6px;
	overflow: hidden;
	font-size: 13px;
	line-height: 20px;
	color: var(--el-text-color-secondary);
	-webkit-box-orient: vertical;
	-webkit-line-clamp: 2;
}

.flow-card__action {
	display: inline-flex;
	grid-column: 1 / -1;
	align-items: center;
	justify-content: flex-end;
	gap: 6px;
	align-self: end;
	margin-top: 18px;
	font-size: 14px;
	font-weight: 600;
	color: var(--el-color-primary);
}

.flow-card__action .el-icon {
	transition: transform 0.2s ease;
}

.flow-card:hover .flow-card__action .el-icon {
	transform: translateX(3px);
}
</style>
