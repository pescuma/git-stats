<template>
	<b-col-sm :cols="colspan">
		<div class="form-group">
			<label for="{{ id }}">{{ label }}</label>
			<select v-el:select id="{{ id }}" class="form-control" multiple="{{ multiple }}">
				<option v-if="!multiple && allowClear"></option>
				<option v-for="o in options" value="{{ o }}">{{ format ? format(o) : o }}</option>
			</select>
		</div>
	</b-col-sm>
</template>

<script>

	var _nexId = 0;

	module.exports = {
		tag: 'b-select2',
		mixins: [require('./mixin-colspan.js')],
		props: {
			id: {
				type: String,
				default: function () {
					return 'b-select2-' + _nexId++;
				}
			},
			label: {
				type: String,
				required: true
			},
			multiple: Boolean,
			allowClear: Boolean,
			emptyText: String,
			options: {
				type: Array,
				required: true
			},
			selected: {
				twoWay: true
			},
			format: Function
		},
		attached: function () {
			var self = this;

			var s = $(this.$els.select);

			s.select2({
				placeholder: this.emptyText,
				allowClear: this.allowClear
			});

			s.val(self.selected).trigger('change');

			s.on('change', function () {
				var candidate = $(this).val();

				if (!self._selectedEquals(self.selected, candidate))
					self.selected = self._fixSelected(candidate);

				return;
			});
		},
		detached: function () {
			$(this.$els.select)
					.off()
					.select2('destroy');
		},
		watch: {
			'selected': function (val, oldVal) {
				var s = $(this.$els.select);

				var current = s.val();

				if (!this._selectedEquals(val, current))
					s.val(val).trigger('change');

				return;
			},
		},
		methods: {
			_fixSelected: function (s) {
				if (this.multiple) {
					if (!s)
						return [];
				} else {
					if (this.allowClear)
						if (!s)
							return null;
				}

				return s;
			},
			_selectedEquals: function (a, b) {
				a = this._fixSelected(a);
				b = this._fixSelected(b);

				if (this.multiple) {
					if (a.length != b.length)
						return false;

					for (var i = 0; i < a.length; ++i)
						if (a[i] !== b[i])
							return false;

					return true;

				} else {
					return a == b;
				}
			}
		}
	};

</script>
