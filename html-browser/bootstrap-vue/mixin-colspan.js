module.exports = {
	props: {
		colspan: {
			validator: function (value) {
				if (!value)
					return true;

				value = parseInt(value);
				return value >= 1 && value <= 12;
			}
		},
	},
	components: {
		'b-col-sm': require('./b-col-sm.vue')
	}
};

