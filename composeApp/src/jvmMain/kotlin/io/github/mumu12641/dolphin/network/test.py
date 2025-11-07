from Crypto.PublicKey import RSA
import requests
from Crypto.Cipher import PKCS1_v1_5
from Crypto.PublicKey import RSA
import json
import re
from base64 import b64encode, b64decode


def get_dict_cookie(r):
    cookies = r.request.headers.get("Cookie")
    if cookies:
        cookies = cookies.split(";")
        cookies = set([i.strip() for i in cookies])
        cookies = {i.split("=")[0]: i.split("=")[1] for i in cookies}
    return cookies


def login(
    username: str,
    password: str,
    target_url: str = "http://pecg.hust.edu.cn/cggl/index1",
):
    params = {"service": target_url}
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36",
    }

    session = requests.session()
    session.headers.update(headers)
    login_html = session.get("https://pass.hust.edu.cn/cas/login", params=params)

    captcha_check = (
        re.search('<div class="ide-code-box">(.*)</div>', login_html.text, re.S)
        is not None
    )
    if captcha_check:
        captcha_img = session.get("https://pass.hust.edu.cn/cas/code", stream=True)

    pub_key = RSA.import_key(
        b64decode(
            json.loads(session.post("https://pass.hust.edu.cn/cas/rsa").text)[
                "publicKey"
            ]
        )
    )
    cipher = PKCS1_v1_5.new(pub_key)
    encrypted_u = b64encode(cipher.encrypt(username.encode())).decode()
    encrypted_p = b64encode(cipher.encrypt(password.encode())).decode()

    form = re.search('<form id="loginForm" (.*)</form>', login_html.text, re.S).group(0)

    nonce = re.search(
        '<input type="hidden" id="lt" name="lt" value="(.*)" />', form
    ).group(1)

    execution = re.search(
        '<input type="hidden" name="execution" value="(.*)" />', form
    ).group(1)

    post_params = {
        "rsa": None,
        "ul": encrypted_u,
        "pl": encrypted_p,
        "code": None if not captcha_check else decaptcha(captcha_img.content).strip(),
        "phoneCode": None,
        "lt": nonce,
        "execution": execution,
        "_eventId": "submit",
    }

    resp = session.post(
        "https://pass.hust.edu.cn/cas/login",
        params=params,
        data=post_params,
        allow_redirects=False,
    )

    url = resp.headers["Location"]
    resp = session.get(url, headers=headers)
    cookies = dict(resp.cookies)

    result = session.get(target_url, headers=headers, cookies=cookies)
    cookies = get_dict_cookie(result)

    return cookies


if __name__ == "__main__":
    user = "M202474221"
    pwd = "PENG20020813peng"
    cookies = login(user, pwd)
    print(cookies)
