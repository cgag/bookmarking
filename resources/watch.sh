#!/bin/bash
#!/bin/bash
# Requires watchr: https://github.com/mynyml/watchr
bundle exec watchr -e 'watch(".*\.less$") { |f| system("lessc styles.less > css/styles.css && echo \"styles.less > css/styles.css\" ") }'


