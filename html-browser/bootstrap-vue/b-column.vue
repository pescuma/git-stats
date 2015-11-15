<script>
	
	module.exports = {
		props: {
			header: {
				required: true
			},
			width: String,
			field: String,
			class: String,
			render: Function
		},
		created: function () {
			this.$parent._registerColumn(this);
		}
	};

</script>
