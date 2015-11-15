<template>
	<b-col-sm :cols="colspan">
		<table v-el:table class="table table-striped table-bordered table-hover" cellspacing="0" width="100%">
			<thead>
			<tr v-for="hr in headers">
				<th v-for="h in hr" rowspan="{{ h.rowspan }}" colspan="{{ h.colspan }}" width="{{ h.width }}">{{ h.title }}</th>
			</tr>
			</thead>
		</table>
	</b-col-sm>
	<slot></slot>
</template>

<script>

	var Vue = require('vue')

	module.exports = {
		mixins: [require('./mixin-colspan.js')],
		data: function () {
			return {
				headers: []
			};
		},
		props: {
			model: Array
		},
		methods: {
			_registerColumn: function (col) {
				if (!this.internal_columns)
					this.internal_columns = [];

				this.internal_columns.push(col);
			},
			_computeHeaders: function () {
				if (!this.internal_columns)
					this.internal_columns = [];

				var result = [];

				var maxDepth = 1;
				for (var i = 0; i < this.internal_columns.length; ++i) {
					var col = this.internal_columns[i];
					if (typeof col.header != 'string')
						maxDepth = Math.max(maxDepth, col.header.length);
				}

				for (var i = 0; i < this.internal_columns.length; ++i) {
					var col = this.internal_columns[i];

					var titles = col.header;
					if (typeof titles == 'string')
						titles = [titles];

					var insideNew = false;

					for (var j = 0; j < titles.length; ++j) {
						var title = titles[j];

						if (result.length <= j)
							result[j] = [];

						var headerLine = result[j];
						var header = headerLine[headerLine.length - 1] || {};

						if (insideNew || header.title !== title) {
							header = {
								rowspan: 1,
								colspan: 0,
								title: title
							};

							headerLine.push(header);

							insideNew = true;
						}

						header.colspan++;

						if (j == titles.length - 1) {
							if (col.width)
								header.width = col.width;
							header.rowspan = maxDepth - j;
						}
					}
				}

				return result;
			},
			_createColumnConfig: function (col) {
				var result = {
					data: col.field || null
				};

				if (col.class)
					result.className = col.class;

				if (col.render) {
					result.render = function (data, type, row) {
						if (type === 'display')
							return col.render(row);
						else
							return row.name;
					};
				}

				return result;
			},
			_updateDataTable: function () {
				var self = this;

				Vue.nextTick(function () {
					var cols = [];
					for (var i = 0; i < self.internal_columns.length; ++i) {
						var col = self.internal_columns[i];

						cols.push(self._createColumnConfig(col));
					}

					self.internal_datatable = $(self.$els.table).DataTable({
						destroy: true,
						searching: false,
						lengthChange: false,
						autoWidth: false,
						data: self.model,
						columns: cols
					});
				})
			},
		},
		attached: function () {
			this.headers = this._computeHeaders();
			this._updateDataTable();
		},
		detached: function () {
			this.internal_datatable.destroy();
			this.internal_datatable = undefined;
		},
		watch: {
			model: function (val, oldVal) {
				this._updateDataTable();
			},
		}
	};

</script>
