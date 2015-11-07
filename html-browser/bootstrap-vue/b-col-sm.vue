<template>
	<div class="col-sm-{{ cols }}" v-if="cols">
		<slot></slot>
	</div>
	<slot v-else></slot>
</template>

<script>

	module.exports = {
		props: ['cols']
	};

</script>
