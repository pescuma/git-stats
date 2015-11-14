<!--suppress ALL -->
<template>
	<b-col-sm :cols="colspan">
		<table v-el:table class="table table-striped table-bordered table-hover" cellspacing="0" width="100%">
			<tr v-for="hr in headers">
				<th v-for="h in hr" rowspan="{{ h.rowspan }}" colspan="{{ h.colspan }}" width="{{ h.width }}">{{ h.title
					}}
				</th>
			</tr>
			<tr v-for="line in rows">
				<td v-for="r in line">{{{ r }}}</td>
			</tr>
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
				internal_columns: []
			};
		},
		props: {
			data: Array,
			pageSize: {
				type: Number,
				default: 10
			},
			pageIndex: {
				type: Number,
				default: 0
			}
		},
		computed: {
			headers: function () {
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
			rows: function () {
				var result = [];
				
				var start = Math.min(Math.max(this.pageSize * this.pageIndex, 0), this.data.length);
				var end = Math.min(Math.max(this.pageSize * (this.pageIndex + 1), start), this.data.length);
				
				for (var i = start; i < end; ++i) {
					var line = [];
					for (var j = 0; j < this.internal_columns.length; ++j) {
						var col = this.internal_columns[j];
						var val = this.data[i][col.data];
						
						if (col.render)
							val = col.render(val);
						else
							val = result.push(this.htmlEncode(val));
						
						line.push(val);
					}
					result.push(line);
				}
				
				return result;
			}
		},
		methods: {
			_registerColumn: function (col) {
				this.internal_columns.push(col);
			},
			htmlEncode: function (str) {
				return html.replace('&', '&amp;')
						   .replace('"', '&quot;')
						   .replace("'", '&#39;')
						   .replace('<', '&lt;')
						   .replace('>', '&gt;');
			}
		},
	};

</script>
