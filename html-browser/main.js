var Vue = require('vue');

Vue.config.debug = true;

new Vue({
	el: '#app',
	components: {
		app: require('./app.vue')
	}
});
