<template>
	<b-col-sm :cols="colspan">
		<ul class="nav nav-tabs" style="margin-bottom: 20px">
			<li v-for="tab in internal_tabs" :class="{ active: tab.active }" @click="activeIndex = $index">
				<a>{{ tab.title }}</a>
			</li>
		</ul>

		<div class="tab-content">
			<slot></slot>
		</div>
	</b-col-sm>
</template>

<script>

	module.exports = {
		mixins: [require('./mixin-colspan.js')],
		data: function () {
			return {
				internal_tabs: []
			};
		},
		props: {
			activeIndex: {
				type: Number,
				default: 0
			}
		},
		methods: {
			_registerTab: function (tab) {
				this.internal_tabs.push(tab);
				return this.internal_tabs.length - 1;
			}
		},
	};

</script>
