<script>
	
	module.exports = {
		tag: 'b-column',
		props: {
			header: {
				required: true
			},
			width: String,
			class: String,
			render: {},
			order: {}
		},
		created: function () {
			this.$parent._registerColumn(this);
		}
	};

</script>
