def test_connection(self):
        test_urls = [
            f"https://pecg.hust.edu.cn/cggl/front/syqk?cdbh={self.booking_config.cdbh}&date={self.booking_config.order_date}"
            f"&starttime={self.booking_config.start_time}&endtime={self.booking_config.end_time}",
            "https://pecg.hust.edu.cn/cggl/front/yuyuexz",
        ]

        for attempt in range(len(test_urls)):
            try:
                test_url = test_urls[attempt]
                logger.debug(
                    f"🔄 Testing connection (attempt {attempt+1}/{len(test_urls)})..."
                )

                response = self.session.get(
                    test_url,
                    headers=self.booking_config.test_headers,
                )

                if response.status_code == 200:
                    logger.info("✅ Connection test successful!")
                    try:
                        cg_csrf_token = re.search(
                            r'cg_csrf_token"\s+value="([a-f0-9-]+)"', response.text
                        ).group(1)

                        if attempt == 0:
                            token = re.search(
                                r'"token":\s*"([a-f0-9]+)"', response.text
                            ).group(1)
                        else:
                            script_pattern = r'<script type="text/javascript">.*?name=\\"token\\".*?value=\\"([a-f0-9]{32})\\".*?</script>'
                            token = re.search(
                                script_pattern, response.text, re.DOTALL
                            ).group(1)

                        return cg_csrf_token, token

                    except (AttributeError, IndexError) as e:
                        logger.error(f"❌ Failed to extract token from response: {e}")
                        continue
                else:
                    logger.warning(
                        f"⚠️ Connection test failed, status code: {response.status_code}"
                    )

            except requests.RequestException as e:
                logger.error(f"❌ Request exception: {e}")

            if attempt < self.max_retries - 1:
                logger.debug(f"⏳ Waiting {self.retry_delay} seconds before retry...")
                time.sleep(self.retry_delay)

        logger.error("❌ Connection test failed, maximum retry attempts reached")
        return None, None