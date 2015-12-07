<template>
	<form style="margin-bottom: 30px">
		<fieldset>
			<legend>Filters</legend>

			<b-row>
				<b-select2 label="Authors" colspan="4" multiple allow-clear empty-text="All" :options="authors" :render="formatAuthor"
						   :model.sync="selectedAuthors"></b-select2>
				<b-select2 label="Languages" colspan="4" multiple allow-clear empty-text="All" :options="languages" :render="formatLanguage"
						   :model.sync="selectedLanguages"></b-select2>
				<b-select2 label="First month" colspan="2" allow-clear empty-text="All" :options="months"
						   :model.sync="firstMonth"></b-select2>
				<b-select2 label="Last month" colspan="2" allow-clear empty-text="All" :options="months"
						   :model.sync="lastMonth"></b-select2>
			</b-row>
		</fieldset>
	</form>

	<b-tabs>
		<b-tab title="Authors">
			<b-row>
				<b-datatable colspan="12" :model="tableAuthors">
					<b-column header="Author" width="20%" :render="renderName" order="name"></b-column>
					<b-column :header="['Lines', 'Total']" render="lines.total" class="text-right"></b-column>
					<b-column :header="['Lines', '%']" render="lines.percent" class="text-right"></b-column>
					<b-column :header="['Lines', 'Code']" render="lines.code" class="text-right"></b-column>
					<b-column :header="['Lines', 'Comment']" render="lines.comment" class="text-right"></b-column>
					<b-column :header="['Lines', 'Empty']" render="lines.empty" class="text-right"></b-column>
					<b-column header="Files" render="files" class="text-right"></b-column>
					<b-column header="Languages" render="languages" class="text-right"></b-column>
					<b-column :header="['Commits', 'Total']" render="commits.total" class="text-right"></b-column>
					<b-column :header="['Commits', 'First']" render="commits.start" class="text-right"></b-column>
					<b-column :header="['Commits', 'Last']" render="commits.end" class="text-right"></b-column>
				</b-datatable>
			</b-row>
			<b-row>
				<b-grid page-size="10" bordered="true" striped="true" hover="true" condensed="true" colspan="12" :model="tableAuthors"
						:order-by="1">
					<b-column header="Author" width="20%" :render="renderName" order="name"></b-column>
					<b-column :header="['Lines', 'Total']" render="lines.total" class="text-right"></b-column>
					<b-column :header="['Lines', '%']" render="lines.percent" class="text-right"></b-column>
					<b-column :header="['Lines', 'Code']" render="lines.code" class="text-right"></b-column>
					<b-column :header="['Lines', 'Comment']" render="lines.comment" class="text-right"></b-column>
					<b-column :header="['Lines', 'Empty']" render="lines.empty" class="text-right"></b-column>
					<b-column header="Files" render="files" class="text-right"></b-column>
					<b-column header="Languages" render="languages" class="text-right"></b-column>
					<b-column :header="['Commits', 'Total']" render="commits.total" class="text-right"></b-column>
					<b-column :header="['Commits', 'First']" render="commits.start" class="text-right"></b-column>
					<b-column :header="['Commits', 'Last']" render="commits.end" class="text-right"></b-column>
				</b-grid>
			</b-row>
		</b-tab>
		<b-tab title="Months">
			<b-grid page-size="10" bordered="true" striped="true" hover="true" condensed="true" colspan="12" :model="tableMonths"
					:order-by="1">
				<b-column header="Month" width="20%" :render="renderName" order="name"></b-column>
				<b-column :header="['Lines', 'Total']" render="lines.total" class="text-right"></b-column>
				<b-column :header="['Lines', '%']" render="lines.percent" class="text-right"></b-column>
				<b-column :header="['Lines', 'Code']" render="lines.code" class="text-right"></b-column>
				<b-column :header="['Lines', 'Comment']" render="lines.comment" class="text-right"></b-column>
				<b-column :header="['Lines', 'Empty']" render="lines.empty" class="text-right"></b-column>
				<b-column header="Files" render="files" class="text-right"></b-column>
				<b-column header="Languages" render="languages" class="text-right"></b-column>
				<b-column header="Commits" render="commits.total" class="text-right"></b-column>
				<b-column header="Authors" render="authors" class="text-right"></b-column>
			</b-grid>
		</b-tab>
		<b-tab title="Languages">
			<b-grid page-size="10" bordered="true" striped="true" hover="true" condensed="true" colspan="12" :model="tableLanguages"
					:order-by="1">
				<b-column header="Language" width="20%" :render="renderName" order="name"></b-column>
				<b-column :header="['Lines', 'Total']" render="lines.total" class="text-right"></b-column>
				<b-column :header="['Lines', '%']" render="lines.percent" class="text-right"></b-column>
				<b-column :header="['Lines', 'Code']" render="lines.code" class="text-right"></b-column>
				<b-column :header="['Lines', 'Comment']" render="lines.comment" class="text-right"></b-column>
				<b-column :header="['Lines', 'Empty']" render="lines.empty" class="text-right"></b-column>
				<b-column header="Files" render="files" class="text-right"></b-column>
				<b-column :header="['Commits', 'Total']" render="commits.total" class="text-right"></b-column>
				<b-column :header="['Commits', 'First']" render="commits.start" class="text-right"></b-column>
				<b-column :header="['Commits', 'Last']" render="commits.end" class="text-right"></b-column>
				<b-column header="Authors" render="authors" class="text-right"></b-column>
			</b-grid>
		</b-tab>
		<b-tab title="Files">
			<b-grid page-size="10" bordered="true" striped="true" hover="true" condensed="true" colspan="12" :model="tableFiles"
					:order-by="1">
				<b-column header="File" width="20%" :render="renderName" order="name"></b-column>
				<b-column :header="['Lines', 'Total']" render="lines.total" class="text-right"></b-column>
				<b-column :header="['Lines', '%']" render="lines.percent" class="text-right"></b-column>
				<b-column :header="['Lines', 'Code']" render="lines.code" class="text-right"></b-column>
				<b-column :header="['Lines', 'Comment']" render="lines.comment" class="text-right"></b-column>
				<b-column :header="['Lines', 'Empty']" render="lines.empty" class="text-right"></b-column>
				<b-column :header="['Commits', 'Total']" render="commits.total" class="text-right"></b-column>
				<b-column :header="['Commits', 'First']" render="commits.start" class="text-right"></b-column>
				<b-column :header="['Commits', 'Last']" render="commits.end" class="text-right"></b-column>
				<b-column header="Authors" render="authors" class="text-right"></b-column>
			</b-grid>
		</b-tab>
	</b-tabs>
</template>

<script>
	require('./vuejs-bootstrap/vuejs-bootstrap.js');

	var COL_LANGUAGE = 0;
	var COL_LINE_TYPE = 1;
	var COL_MONTH = 2;
	var COL_COMMIT = 3;
	var COL_AUTHOR = 4;
	var COL_FILE = 5;

	var EMPTY = "Empty";
	var CODE = "Code";
	var COMMENT = "Comment";

	var db = {
		all: require("./data.js").create(),
		selectedAuthors: [],
		selectedLanguages: [],
		firstMonth: null,
		lastMonth: null
	};

	var totalLines = db.all.sum();

	$(document).ready(function () {
		$('select').select2({
			placeholder: "All",
			allowClear: true
		});
	});

	var palette = ["#4bb2c5", "#c5b47f", "#EAA228", "#579575", "#839557", "#958c12", "#953579", "#4b5de4", "#d8b83f", "#ff5800", "#0085cc"];

	function formatAuthor(a) {
		return a || '<Umblamable>';
	}

	function formatLanguage(a) {
		return a || '<Unknown>';
	}

	function createAuthorsTab(data) {
		var table = createTable(data, COL_AUTHOR);

		createFullGrid('authors-table', table, {authors: false});
		createDonuts('authors', table);
		createPerMonthGraphs("authors", table)
	}

	function createMonthsTab(data) {
		var table = createTable(data, COL_MONTH);
		removeColors(table);

		createFullGrid('months-table', table, {months: false});
		createPerMonthGraph('months-graph', data);
	}

	function createLanguagesTab(data) {
		var table = createTable(data, COL_LANGUAGE);

		createFullGrid('langs-table', table, {languages: false});
		createDonuts('langs', table);
		createPerMonthGraphs("langs", table)
	}

	function createFilesTab(data) {
		var table = createTable(data, COL_FILE);
		removeColors(table);

		createFullGrid('files-table', table, {files: false, languages: false});
	}

	function removeColors(table) {
		for (var i = 0; i < table.length; ++i)
			delete table[i].color;
	}

	function createTable(data, col) {
		var result = [];
		var i;

		var items = data.getDistinct(col);
		for (i = 0; i < items.length; ++i) {
			var item = items[i];

			var itemData = data.filter(col, item);

			var mr = getFirstAndLastMonths(itemData);

			var itemLines = itemData.sum();

			result.push({
				data: itemData,
				name: formatAuthor(item),
				lines: {
					total: itemLines,
					percent: percent(itemLines, totalLines),
					code: itemData.filter(COL_LINE_TYPE, CODE).sum(),
					comment: itemData.filter(COL_LINE_TYPE, COMMENT).sum(),
					empty: itemData.filter(COL_LINE_TYPE, EMPTY).sum()
				},
				files: itemData.getDistinct(COL_FILE).count(),
				languages: itemData.getDistinct(COL_LANGUAGE).count(),
				commits: {
					total: itemData.getDistinct(COL_COMMIT).count(),
					start: mr[0],
					end: mr[1]
				},
				authors: itemData.getDistinct(COL_AUTHOR).count()
			});
		}

		result = result.sortBy(function (a) {
			return a.lines.total;
		}, true);

		for (i = 0; i < result.length; ++i) {
			if (result[i].name === "<Unblamable>")
				result[i].color = "#D1DDDF";
			else
				result[i].color = palette[i % palette.length];
		}

		return result;
	}

	function getFirstAndLastMonths(data) {
		var result = data.getDistinct(COL_MONTH)
				.exclude("");
		result.sort();

		if (result.length < 1)
			return ["unknown", "unknown"];
		else
			return [result[0], result[result.length - 1]];
	}

	function getMonthRange(data) {
		var fl = getFirstAndLastMonths(data);
		if (fl[0] === "unknown")
			return [];

		return Date.range(fl[0], fl[1])
				.every('month')
				.map(function (m) {
					return m.format('{yyyy}-{MM}');
				});
	}

	function percent(count, total) {
		return parseInt(count * 1000 / total) / 10;
	}

	function createFullGrid(div, table, opts) {
		var cols = [
			{
				data: null,
				render: function (data, type, row) {
					if (type === 'display') {
						if (row.color)
							return "<span style='background-color: " + row.color
									+ "; width: 15px; height: 15px; float: left; border: 1px solid #aaa; margin-right: 4px;'></span> "
									+ htmlEscape(row.name);
						else
							return htmlEscape(row.name);
					}

					return row.name;
				}
			},
			{data: 'lines.total', className: 'text-right'},
			{data: 'lines.percent', className: 'text-right'},
			{data: 'lines.code', className: 'text-right'},
			{data: 'lines.comment', className: 'text-right'},
			{data: 'lines.empty', className: 'text-right'}
		];

		if (opts.files !== false)
			cols.push({data: 'files', className: 'text-right'});

		if (opts.languages !== false)
			cols.push({data: 'languages', className: 'text-right'});

		cols.push({data: 'commits.total', className: 'text-right'});

		if (opts.months !== false) {
			cols.push({data: 'commits.start'});
			cols.push({data: 'commits.end'});
		}

		if (opts.authors !== false)
			cols.push({data: 'authors', className: 'text-right'});

		$('#' + div).DataTable({
			destroy: true,
			data: table,
			order: [[1, "desc"]],
			columns: cols
		});
	}

	function htmlEscape(str) {
		return String(str)
				.replace(/&/g, '&amp;')
				.replace(/"/g, '&quot;')
				.replace(/'/g, '&#39;')
				.replace(/</g, '&lt;')
				.replace(/>/g, '&gt;');
	}

	function createDonuts(div, table) {
		var itens = ["Total", "Code", "Comment", "Empty"];

		for (var i = 0; i < itens.length; ++i) {
			var id = div + "-donut-" + i;

			$("#" + div).append("<div class='col-md-3'><div id='" + id + "' style='width: 100%; height: 300px'></div></div>");

			createDonut(id, table, itens[i].toLowerCase(), "Lines: " + itens[i]);
		}
	}

	function createDonut(div, table, field, title) {
		var dounutData = table.map(function (a) {
			return [a.name, a.lines[field]];
		});

		$("#" + div).empty();
		$.jqplot(div, [dounutData], {
			title: title,
			seriesDefaults: {
				renderer: $.jqplot.DonutRenderer,
				rendererOptions: {
					sliceMargin: 3,
					startAngle: -90,
					showDataLabels: true
				}
			},
			grid: {
				shadow: false,
				background: "transparent",
				borderColor: "transparent"
			},
			seriesColors: table.map(function (a) {
				return a.color;
			})
		});
	}

	function createPerMonthGraphs(div, table) {
		for (var i = 0; i < table.length; ++i) {
			var item = table[i];
			var id = div + "-month-" + i;

			$("#" + div).append("<div class='col-md-12'>" +
					"<div id='" + id + "' style='width: 100%; height: 200px'></div>" +
					"</div>");

			createPerMonthGraph(id, item.data, item.name);
		}
	}

	function createPerMonthGraph(div, data, title) {
		var code = prepareForGraphPerMonth(data.filter(COL_LINE_TYPE, CODE));
		var comments = prepareForGraphPerMonth(data.filter(COL_LINE_TYPE, COMMENT));
		var empty = prepareForGraphPerMonth(data.filter(COL_LINE_TYPE, EMPTY));

		$("#" + div).empty();
		$.jqplot(div, [code, comments, empty], {
			title: title,
			axes: {
				xaxis: {
					renderer: $.jqplot.DateAxisRenderer,
					tickOptions: {
						formatString: '%m/%Y'
					}
				},
				yaxis: {
					min: 0
				}
			},
			stackSeries: true,
			showMarker: false,
			seriesDefaults: {
				fill: true,
				shadow: false
			},
			grid: {
				shadow: false,
				background: "transparent",
				borderColor: "transparent"
			},
			series: [
				{label: 'Code'},
				{label: 'Comments'},
				{label: 'Empty lines'}
			],
			legend: {
				show: true,
				placement: 'outsideGrid'
			},
			highlighter: {
				show: true,
				sizeAdjust: 10,
				tooltipLocation: 'n'
			},
			seriesColors: ["#4BB2C5", "#ADC8A7", "#F8EED5"]
		});
	}

	function prepareForGraphPerMonth(data) {
		data = data.groupBy(COL_MONTH);

		months.forEach(function (m) {
			data.inc(0, m);
		});

		return data.map(function (val, month) {
					return [Date.create(month), val];
				})
				.sortBy(function (i) {
					return i[0]
				});

	}

	module.exports = {
		data: function () {
			return db;
		},
		computed: {
			authors: function () {
				return this.all.getDistinct(COL_AUTHOR)
						.sortBy();
			},
			languages: function () {
				return this.all.getDistinct(COL_LANGUAGE)
						.sortBy();
			},
			months: function () {
				return getMonthRange(this.all);
			},
			db: function () {
				var self = this;
				var result = self.all;

				if (self.selectedAuthors.length)
					result = result.filter(COL_AUTHOR, function (a) {
						return self.selectedAuthors.any(a);
					});

				if (self.selectedLanguages.length)
					result = result.filter(COL_LANGUAGE, function (a) {
						return self.selectedLanguages.any(a);
					});

				if (self.firstMonth)
					result = result.filter(COL_MONTH, function (a) {
						return a >= self.firstMonth;
					});

				if (self.lastMonth)
					result = result.filter(COL_MONTH, function (a) {
						return a <= self.lastMonth;
					});

				return result;
			},
			tableAuthors: function () {
				return createTable(this.db, COL_AUTHOR);
			},
			tableMonths: function () {
				return createTable(this.db, COL_MONTH);
			},
			tableLanguages: function () {
				return createTable(this.db, COL_LANGUAGE);
			},
			tableFiles: function () {
				return createTable(this.db, COL_FILE);
			}
		},
		methods: {
			formatAuthor: formatAuthor,
			formatLanguage: formatLanguage,
			renderName: function (row) {
				if (row.color)
					return "<span style='background-color: " + row.color
							+ "; width: 15px; height: 15px; float: left; border: 1px solid #aaa; margin-right: 4px;'></span> "
							+ htmlEscape(row.name);
				else
					return htmlEscape(row.name);
			}
		}
	};

</script>
