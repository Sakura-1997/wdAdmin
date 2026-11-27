<script setup lang="ts">
import type { FlowVO } from '/@/api/flow/flow/types';
import FlowCard from './FlowCard.vue';
import type { FlowGroupView } from '../types';

interface Props {
	groups: FlowGroupView[];
	keyword: string;
}

defineProps<Props>();

const emit = defineEmits<{
	start: [flow: FlowVO];
}>();
</script>

<template>
	<section class="flow-catalog" aria-live="polite">
		<template v-if="groups.length">
			<div class="flow-catalog__grid">
				<template v-for="group in groups">
					<FlowCard
						v-for="flow in group.items"
						:key="`${group.key}-${flow.flowId || flow.name || flow.formId}`"
						:flow="flow"
						@start="emit('start', $event)"
					/>
				</template>
			</div>
		</template>
		<el-empty v-else :image-size="96" :description="keyword ? `未找到与“${keyword}”匹配的流程` : '当前分组暂无可发起流程'" />
	</section>
</template>

<style scoped>
.flow-catalog {
	min-width: 0;
	height: 100%;
	min-height: 0;
	padding: 22px;
	overflow-y: auto;
	background: var(--el-bg-color);
}

.flow-catalog__grid {
	display: grid;
	grid-template-columns: repeat(3, minmax(0, 1fr));
	gap: 16px;
}

.flow-catalog :deep(.el-empty) {
	padding-top: 80px;
}

@media (max-width: 1200px) {
	.flow-catalog__grid {
		grid-template-columns: repeat(2, minmax(0, 1fr));
	}
}

@media (max-width: 900px) {
	.flow-catalog {
		height: auto;
		min-height: 420px;
		padding: 16px;
		overflow: visible;
	}
}

@media (max-width: 720px) {
	.flow-catalog__grid {
		grid-template-columns: 1fr;
	}
}
</style>
