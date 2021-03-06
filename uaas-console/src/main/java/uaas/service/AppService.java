package uaas.service;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import uaas.domain.App;
import uaas.domain.Resource;
import uaas.domain.Role;
import uaas.exception.BussinessException;
import uaas.repo.AppRepo;
import uaas.repo.ResourceRepo;
import uaas.repo.RoleRepo;

@Service
public class AppService {

	@Autowired
	private AppRepo appRepo;
	@Autowired
	private RoleRepo roleRepo;
	@Autowired
	private ResourceRepo resourceRepo;

	/**
	 * 获取应用列表<br>
	 * 
	 * 
	 * @param page
	 * @param size
	 * @return
	 */
	public Page<App> list(int page, int size) {
		// 最多100条
		size = size > 100 ? 100 : size;
		Page<App> apps = appRepo
				.findByStateNot(-1, new PageRequest(page, size));
		return apps;
	}

	/**
	 * 创建应用<br>
	 * 
	 * <pre>
	 * 1、应用名称
	 * 2、编码不能重复
	 * 3、默认管理员是当前创建的用户
	 * </pre>
	 * 
	 * @param app
	 */
	@Transactional
	public Long create(App app) {

		App dbApp = appRepo.findByName(app.getName());
		if (null != dbApp) {
			throw new BussinessException("name_exist_exception", "该应用名称已存在");
		}

		dbApp = appRepo.findByCode(app.getCode());
		if (null != dbApp) {
			throw new BussinessException("code_exist_exception", "该应用编码已存在");
		}

		app.setDateCreated(new Date());
		app.setState(1);
		app.setLastUpdated(new Date());

		appRepo.save(app);
		return app.getId();
	}

	/**
	 * 更新应用<br>
	 * 
	 * <pre>
	 * 1、只能更新基本属性
	 * 2、不能更新状态
	 * </pre>
	 * 
	 * @param app
	 */
	@Transactional
	public void update(App app) {
		App dbApp = get(app.getId());
		dbApp.setCode(app.getCode());
		dbApp.setName(app.getName());
		dbApp.setLastUpdated(new Date());
		dbApp.setDescription(app.getDescription());
		dbApp.setSecret(app.getSecret());
		dbApp.setUrl(app.getUrl());
	}

	/**
	 * 更新状态<br>
	 * 
	 * <pre>
	 * 1、接口对外式透明的，只需要一个主键即可
	 * 2、当默认为启用状态（1），调用此方法会设置（0）
	 * 3、当默认为禁用状态（0），调用此方法会设置（1）
	 * </pre>
	 * 
	 * @param id
	 */
	@Transactional
	public void enabled(Long id) {
		App app = appRepo.findOne(id);

		if (null == app) {
			throw new BussinessException("app_not_exist", "应用不存在");
		}

		if (0 == app.getState()) {
			app.setState(1);
		} else {
			app.setState(0);
		}
	}

	/**
	 * 删除应用<br>
	 * 
	 * <pre>
	 * 1、只是逻辑删除
	 * 2、状态置位删除状态（-1）
	 * 3、删除角色（-1）
	 * 4、删除资源（-1）
	 * </pre>
	 * 
	 * @param id
	 */
	@Transactional
	public void delete(Long id) {
		App app = appRepo.findOne(id);
		if (null == app) {
			throw new BussinessException("app_not_exist", "应用不存在");
		}
		app.setState(-1);
		List<Role> roles = roleRepo.findByAppId(id);
		for (Role role : roles) {
			role.setState(-1);
		}
		roleRepo.save(roles);

		List<Resource> resources = resourceRepo.findByAppId(id);
		for (Resource resource : resources) {
			resource.setState(-1);
		}
		resourceRepo.save(resources);

		appRepo.save(app);
	}

	/**
	 * 获取一个应用
	 * 
	 * @param id
	 * @return
	 */
	public App get(Long id) {
		App app = appRepo.findByIdAndStateNot(id, -1);

		if (null == app) {
			throw new BussinessException("app_not_found_exception",
					"该应用不存在或者已经被删除");
		}
		return app;
	}

	public Page<App> findByNameLike(String name, int page, int size) {
		size = size > 100 ? 100 : size;
		Page<App> apps = appRepo.findByNameLike("%" + name + "%",
				new PageRequest(page, size));
		return apps;
	}

	/**
	 * 获取所有应用
	 * 
	 * @return 所有可用的应用
	 */
	public List<App> findAll() {
		Page<App> apps = appRepo.findByStateNot(-1, null);
		return apps.getContent();
	}

}
