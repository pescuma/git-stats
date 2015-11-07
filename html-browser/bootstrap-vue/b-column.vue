<script>

	module.exports = {
		props: {
			header: {
				required: true
			},
			width: String,
			data: String,
			class: String,
			render: Function
		},
		created: function () {
			this.$parent._registerColumn(this);
		}
	};

</script>
