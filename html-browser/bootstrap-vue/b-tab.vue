<template>
	<div class="tab-pane active" v-if="active">
		<slot></slot>
	</div>
</template>

<script>

	module.exports = {
		data: function () {
			return {
				index: null
			};
		},
		props: {
			title: {
				type: String,
				required: true
			}
		},
		computed: {
			active: function () {
				return this.index === this.$parent.activeIndex;
			}
		},
		created: function () {
			this.index = this.$parent._registerTab(this);
		}
	};

</script>
