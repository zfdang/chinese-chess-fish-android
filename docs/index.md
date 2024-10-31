## 所有的帖子
<ul>
  {% for post in site.posts %}
    <li>
      <a href="{{ post.url }}">{{ post.title }}</a>
      {{ post.excerpt }}
    </li>
  {% endfor %}
</ul>


## 所有的文章
<ul>
  {% for page in site.pages %}
    <li>
      <a href="{{ page.url }}">{{  page.title }}</a>
      {{ page.excerpt }}
    </li>
  {% endfor %}
</ul>
