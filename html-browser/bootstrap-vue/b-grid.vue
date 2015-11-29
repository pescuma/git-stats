<style scoped>
	table.table thead tr th.order {
		position: relative;
		padding-right: 20px;
	}

	table.table thead tr th.order-desc:after {
		opacity: 1;
		content: "\e252";
	}

	table.table thead tr th.order-asc:after {
		opacity: 1;
		content: "\e253";
	}

	table.table thead tr th.order-no:after {
		opacity: 0.4;
		content: "\e252";
	}

	table.table thead tr th.order:after {
		font-family: "Glyphicons Halflings";
		font-size: 10px;
		position: absolute;
		top: 8px;
		right: 5px;
		display: block;
		box-sizing: border-box;
	}
</style>

<template>
	<b-col-sm :cols="colspan">
		<table v-el:table :class="tableClass" :style="tableStyle" cellspacing="0" width="100%">
			<thead>
			<tr v-for="hr in headers">
				<th v-for="h in hr" rowspan="{{ h.rowspan }}" colspan="{{ h.colspan }}" width="{{ h.width }}" :style="thStyle"
					:class="{ 'order': h.order, 'order-no': h.order && h.field != orderBy }">{{ h.title }}
				</th>
			</tr>
			</thead>
			<tbody>
			<tr v-for="line in rows">
				<td v-for="r in line" class="{{ r.class }}" :style="trStyle">{{{ r.value }}}</td>
			</tr>
			</tbody>
		</table>
		<nav v-if="pagingItems">
			<ul class="pagination">
				<li :class="{ disabled: pageIndex == 0 }"><a @click="pageIndex = Math.max(pageIndex - 1, 0)">&laquo;</a></li>
				<li v-for="i in pagingItems" :class="{ active: pageIndex == i.val }"><a @click="pageIndex = i.val">{{ i.name }}</a></li>
				<li :class="{ disabled: pageIndex == lastPage }"><a @click="pageIndex = Math.min(pageIndex + 1, lastPage)">&raquo;</a></li>
			</ul>
		</nav>
		<slot></slot>
	</b-col-sm>
</template>

<script>

	var Vue = require('vue')

	function _addTitleToWrapedElement() {
		var $el = $(this);
		var title = $el.attr('title');
		var needs = (this.scrollWidth > this.clientWidth);
		if (needs) {
			if (!title) {
				$el.attr('title', $el.text()
						.trim());
			}
		} else {
			if (title == $el.text()
							.trim()) {
				$el.removeAttr('title');
			}
		}
	}

	module.exports = {
		mixins: [require('./mixin-colspan.js')],
		data: function () {
			return {
				internal_columns: []
			};
		},
		props: {
			model: Array,
			pageSize: {
				default: null
			},
			pageIndex: {
				default: 0
			},
			orderBy: String,
			orderAsc: {
				default: true
			},
			wrapLines: {
				default: false
			},
			striped: {
				default: true
			},
			bordered: {
				default: true
			},
			hover: {
				default: true
			},
			condensed: {
				default: false
			},
		},
		computed: {
			tableClass: function () {
				return {
					'table': true,
					'table-striped': this.striped == 'true',
					'table-bordered': this.bordered == 'true',
					'table-hover': this.hover == 'true',
					'table-condensed': this.condensed == 'true'
				};
			},
			tableStyle: function () {
				var result = {};

				if (this.wrapLines != 'true') {
					result['table-layout'] = 'fixed';
				}

				return result;
			},
			thStyle: function () {
				var result = {
					'text-align': 'center',
					'vertical-align': 'top !important',
				};

				if (this.wrapLines != 'true') {
					result['white-space'] = 'nowrap';
					result['overflow'] = 'hidden';
					result['text-overflow'] = 'ellipsis';
				}

				return result;
			},
			trStyle: function () {
				var result = {};

				if (this.wrapLines != 'true') {
					result['white-space'] = 'nowrap';
					result['overflow'] = 'hidden';
					result['text-overflow'] = 'ellipsis';
				}

				return result;
			},
			headers: function () {
				var result = [];

				var maxDepth = 1;
				for (var i = 0; i < this.internal_columns.length; ++i) {
					var col = this.internal_columns[i];
					if (typeof col.header != 'string') {
						maxDepth = Math.max(maxDepth, col.header.length);
					}
				}

				for (var i = 0; i < this.internal_columns.length; ++i) {
					var col = this.internal_columns[i];

					var titles = col.header;
					if (typeof titles == 'string') {
						titles = [titles];
					}

					var insideNew = false;

					for (var j = 0; j < titles.length; ++j) {
						var title = titles[j];

						if (result.length <= j) {
							result[j] = [];
						}

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
							if (col.width) {
								header.width = col.width;
							}
							header.rowspan = maxDepth - j;
							header.order = !!col.field;
						}
					}
				}

				return result;
			},
			sortedData: function () {
				var result = this.model.slice();

				if (this.orderBy) {
					var field = this.orderBy;
					var asc = this.orderAsc == 'true';

					result.sort(function (a, b) {
						var va = a[field];
						var vb = b[field];

						if (va.last_nom < vb.last_nom) {
							return asc ? -1 : 1;
						}

						if (va.last_nom > vb.last_nom) {
							return asc ? 1 : -1;
						}

						return 0;
					});
				}

				return result;
			},
			rows: function () {
				var result = [];

				var data = this.sortedData;

				var pi = parseInt(this.pageIndex);
				var ps = parseInt(this.pageSize);

				var start, end;
				if (this.pageSize) {
					start = Math.min(Math.max(ps * pi, 0), data.length);
					end = Math.min(start + ps, data.length);
				} else {
					start = 0;
					end = data.length;
				}

				for (var i = start; i < end; ++i) {
					var line = [];
					var dataLine = data[i];
					for (var j = 0; j < this.internal_columns.length; ++j) {
						var col = this.internal_columns[j];
						var val;

						if (col.render) {
							val = col.render(dataLine);
						} else if (col.field) {
							val = this.htmlEncode(this.getProperty(dataLine, col.field));
						} else {
							val = '';
						}

						line.push({
							value: val,
							class: col.class
						});
					}
					result.push(line);
				}

				return result;
			},
			lastPage: function () {
				if (!this.pageSize) {
					return 0;
				}

				if (!this.model.length) {
					return 0;
				}

				return Math.floor((this.model.length - 1) / this.pageSize);
			},
			pagingItems: function () {
				if (!this.pageSize) {
					return null;
				}

				if (!this.model.length) {
					return null;
				}

				var borders = 2;

				var min = Math.max(this.pageIndex - borders, 0);
				var max = Math.min(min + 2 * borders, this.lastPage);
				min = Math.max(max - 2 * borders, 0);

				if (min == max) {
					return null;
				}

				var result = [];
				for (var i = min; i <= max; ++i)
					result.push({
						name: i + 1,
						val: i
					});
				return result;
			}
		},
		methods: {
			_registerColumn: function (col) {
				this.internal_columns.push(col);
			},
			htmlEncode: function (str) {
				if (str === undefined || str === null)
					str = '';

				return str.toString()
						.replace('&', '&amp;')
						.replace('"', '&quot;')
						.replace("'", '&#39;')
						.replace('<', '&lt;')
						.replace('>', '&gt;');
			},
			getProperty: function (obj, prop) {
				prop = prop.replace(/\[(\w+)\]/g, '.$1'); // convert indexes to properties
				prop = prop.replace(/^\./, '');           // strip a leading dot
				var a = prop.split('.');
				for (var i = 0, n = a.length; i < n; ++i) {
					var k = a[i];
					if (k in obj)
						obj = obj[k];
					else
						return null;
				}
				return obj;
			}
		},
		attached: function () {
			if (this.wrapLines != 'true') {
				var self = this;

				Vue.nextTick(function () {
					$(self.$els.table)
							.on('mouseenter', 'th,td', _addTitleToWrapedElement);
				});
			}
		},
		detached: function () {
			$(this.$els.table)
					.off('mouseenter', 'th,td', _addTitleToWrapedElement);
		},
	};

</script>
