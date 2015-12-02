<template>
	<b-col-sm :cols="colspan">
		<ul class="nav nav-tabs" style="margin-bottom: 20px">
			<li v-for="tab in tabs" :class="{ active: tab.active }" @click="tab.active = true">
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
		tag: 'b-tabs',
		mixins: [require('./mixin-colspan.js')],
		data: function () {
			return {
				tabs: []
			};
		},
		ready: function () {
			var self = this;

			for (var i = 0; i < this.$children.length; ++i) {
				var child = this.$children[i];

				if (child.$options.tag != 'b-tab')
					continue;

				child._index = this.tabs.length;
				this.tabs.push(child.$data);

				child.$watch('active', function (active) {
					if (!active)
						return;

					for (var j = 0; j < self.tabs.length; ++j) {
						if (j === this._index)
							continue;

						self.tabs[j].active = false;
					}
				});
			}

			if (this.tabs.length)
				this.tabs[0].active = true;
		}
	};

</script>
