<script setup lang="ts">
const props = defineProps<{
	primaryLabel: string;
	previousLabel: string;
}>();

const emit = defineEmits<{
	previous: [];
	save: [];
	primary: [];
}>();
</script>

<template>
	<footer class="create-flow-footer">
		<el-button data-testid="flow-previous-action" @click="emit('previous')">{{ props.previousLabel }}</el-button>
		<div class="create-flow-footer__actions">
			<el-button data-testid="flow-footer-save" @click="emit('save')">保存草稿</el-button>
			<el-button type="primary" data-testid="flow-footer-primary" @click="emit('primary')">
				{{ props.primaryLabel }}
			</el-button>
		</div>
	</footer>
</template>

<style scoped>
.create-flow-footer {
	display: flex;
	align-items: center;
	justify-content: space-between;
	min-height: 60px;
	padding: 10px 24px;
	border-top: 1px solid var(--el-border-color-lighter);
	background: var(--el-bg-color);
}

.create-flow-footer__actions {
	display: flex;
	align-items: center;
	gap: 10px;
}

@media (max-width: 900px) {
	.create-flow-footer {
		padding: 10px 16px;
	}
}
</style>
