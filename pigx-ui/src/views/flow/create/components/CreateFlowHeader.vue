<script setup lang="ts">
import { ArrowLeft, CircleCheck, Check } from '@element-plus/icons-vue';

interface StepItem {
	title: string;
}

const props = defineProps<{
	activeStep: number;
	steps: StepItem[];
	savedLabel: string;
}>();

const emit = defineEmits<{
	back: [];
	navigate: [step: number];
}>();
</script>

<template>
	<header class="create-flow-header">
		<div class="create-flow-header__identity">
			<button type="button" class="create-flow-header__back" aria-label="返回流程管理" @click="emit('back')">
				<el-icon><ArrowLeft /></el-icon>
				<span>创建流程</span>
			</button>
			<span class="create-flow-header__saved">
				<el-icon><CircleCheck /></el-icon>
				{{ props.savedLabel }}
			</span>
		</div>

		<nav class="create-flow-steps" aria-label="创建流程步骤">
			<span v-for="(step, index) in props.steps" :key="step.title" class="create-flow-step-group">
				<button
					type="button"
					class="create-flow-step"
					:class="{ 'is-active': props.activeStep === index, 'is-complete': index < props.activeStep }"
					:aria-current="props.activeStep === index ? 'step' : undefined"
					:data-testid="`flow-step-${index + 1}`"
					@click="emit('navigate', index)"
				>
					<span class="create-flow-step__index">
						<el-icon v-if="index < props.activeStep"><Check /></el-icon>
						<template v-else>{{ index + 1 }}</template>
					</span>
					<span class="create-flow-step__label">{{ $t(step.title) }}</span>
				</button>
				<span v-if="index < props.steps.length - 1" class="create-flow-steps__line" aria-hidden="true"></span>
			</span>
		</nav>
	</header>
</template>

<style scoped>
.create-flow-header {
	display: grid;
	grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
	align-items: center;
	gap: 24px;
	min-height: 72px;
	padding: 0 24px;
	border-bottom: 1px solid var(--el-border-color-lighter);
	background: var(--el-bg-color);
}

.create-flow-header::after {
	min-width: 0;
	content: '';
}

.create-flow-header__identity,
.create-flow-header__back,
.create-flow-header__saved,
.create-flow-steps,
.create-flow-step-group,
.create-flow-step {
	display: flex;
	align-items: center;
}

.create-flow-header__identity {
	min-width: 0;
	gap: 16px;
}

.create-flow-header__back {
	gap: 10px;
	min-width: 0;
	padding: 8px 0;
	border: 0;
	color: var(--el-text-color-primary);
	background: transparent;
	font-size: 18px;
	font-weight: 600;
	cursor: pointer;
}

.create-flow-header__back:focus-visible,
.create-flow-step:focus-visible {
	outline: 2px solid var(--el-color-primary-light-5);
	outline-offset: 3px;
	border-radius: var(--el-border-radius-base);
}

.create-flow-header__saved {
	gap: 5px;
	white-space: nowrap;
	color: var(--el-text-color-secondary);
	font-size: 13px;
}

.create-flow-header__saved .el-icon {
	color: var(--el-color-success);
}

.create-flow-steps {
	justify-content: center;
	gap: 10px;
}

.create-flow-step {
	position: relative;
	gap: 8px;
	padding: 22px 4px 20px;
	border: 0;
	color: var(--el-text-color-regular);
	background: transparent;
	font-size: 15px;
	font-weight: 500;
	cursor: pointer;
}

.create-flow-step::after {
	position: absolute;
	right: 0;
	bottom: -1px;
	left: 0;
	height: 2px;
	background: transparent;
	content: '';
}

.create-flow-step.is-active {
	color: var(--el-color-primary);
}

.create-flow-step.is-active::after {
	background: var(--el-color-primary);
}

.create-flow-step__index {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	width: 28px;
	height: 28px;
	border: 1px solid var(--el-border-color);
	border-radius: 50%;
	font-size: 14px;
	line-height: 1;
}

.create-flow-step.is-active .create-flow-step__index {
	border-color: var(--el-color-primary);
	color: var(--el-color-white);
	background: var(--el-color-primary);
}

.create-flow-step.is-complete .create-flow-step__index {
	border-color: var(--el-color-primary-light-5);
	color: var(--el-color-primary);
	background: var(--el-color-primary-light-9);
}

.create-flow-steps__line {
	width: 34px;
	height: 1px;
	background: var(--el-border-color);
}

@media (max-width: 1180px) {
	.create-flow-header {
		gap: 14px;
		padding: 0 16px;
	}

	.create-flow-header__saved {
		display: none;
	}

	.create-flow-steps__line {
		width: 18px;
	}
}

@media (max-width: 900px) {
	.create-flow-header__back span,
	.create-flow-step__label {
		display: none;
	}

	.create-flow-header {
		grid-template-columns: 36px 1fr 36px;
	}
}
</style>
