<template>
	<div class="tab-pane active" v-if="active">
		<slot></slot>
	</div>
</template>

<script>

	module.exports = {
		tag: 'b-tab',
		props: {
			title: {
				type: String,
				required: true
			},
			active: Boolean
		}
	};

</script>
